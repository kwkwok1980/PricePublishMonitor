package com.morgan.stanley;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

public class PricePublishMonitor {
	
	public static final String COMPANY_UPDATE_IS_SLOW = "**** Company update is slow ****.";
	public static final String COMPANY_UPDATE_IS_INVALID = "**** Company update is invalid ****.";
	public static final String BANK_VS_COMPANY = "Bank [%s] vs Company [%s].";
	
	final static Logger logger = Logger.getLogger(PricePublishMonitor.class);
	
	private PriceUpdateService priceUpdateService;
	
	private AlertService alertService;
	
	private ScheduledExecutorService scheduledExecutorService;
	
	private ConcurrentHashMap<String, Message> bankMessages = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Message> companyMessages = new ConcurrentHashMap<>();
	private PriceListener bankPriceListener = null;
	private PriceListener companyPriceListener = null;
	private AtomicLong sequence = null;
	
	private long throttle;
	
	public PricePublishMonitor(PriceUpdateService pPriceUpdateService, AlertService pAlertService, long pThrottle){
		priceUpdateService = pPriceUpdateService;
		alertService = pAlertService;
		throttle = pThrottle;
		
		scheduledExecutorService = Executors.newScheduledThreadPool(2);
		bankMessages = new ConcurrentHashMap<>();
		companyMessages = new ConcurrentHashMap<>();
		bankPriceListener = createBankPriceListener();
		companyPriceListener = createCompanyPriceListener();
		sequence = new AtomicLong();
	}
	
	public void start(){
		priceUpdateService.subscribeToBankPriceUpdates(bankPriceListener);
		priceUpdateService.subscribeToCompanyPriceUpdates(companyPriceListener);
	}
	
	public void stop(){
		scheduledExecutorService.shutdown();
		try {
			scheduledExecutorService.awaitTermination(60, TimeUnit.MINUTES);
		}
		catch (InterruptedException e) {
			logger.error(e);
		}
	}
	
	public PriceListener getBankPriceListener() {
		return bankPriceListener;
	}

	public PriceListener getCompanyPriceListener() {
		return companyPriceListener;
	}
	
	private void receiveBankPrice(String pSymbol, double pPrice){
		long lSequence = sequence.incrementAndGet();
		long lTime = System.currentTimeMillis();
		Runnable lTask = createCheckBankPriceTask(pSymbol, lSequence);
		
		Message lOldMessage = bankMessages.get(pSymbol);
		Message lNewMessage = new Message(pSymbol, pPrice, lTime, lSequence);
		
		if(lOldMessage == null){
			bankMessages.put(pSymbol, lNewMessage);
			scheduledExecutorService.schedule(lTask, throttle + 100, TimeUnit.MILLISECONDS);
		}
		else{
			if(lNewMessage.getPrice() != lOldMessage.getPrice()){
				bankMessages.put(pSymbol, lNewMessage);
				scheduledExecutorService.schedule(lTask, throttle + 100, TimeUnit.MILLISECONDS);
			}
		}		
	}
	
	private void receiveCompanyPrice(String pSymbol, double pPrice){
		long lSequence = sequence.incrementAndGet();
		long lTime = System.currentTimeMillis();
		Runnable lTask = createCheckCompanyPriceTask(pSymbol, lSequence);
		
		Message lOldMessage = companyMessages.get(pSymbol);
		Message lNewMessage = new Message(pSymbol, pPrice, lTime, lSequence);
		
		if(lOldMessage == null){
			companyMessages.put(pSymbol, lNewMessage);
			scheduledExecutorService.submit(lTask);
		}
		else{
			if(lOldMessage.getPrice() != lNewMessage.getPrice()){
				companyMessages.put(pSymbol, lNewMessage);
				scheduledExecutorService.submit(lTask);
			}
			else{
				String lError = String.format(COMPANY_UPDATE_IS_INVALID + " " + BANK_VS_COMPANY, getBankMessage(pSymbol), lNewMessage);
				alertService.alert(lError);
				logger.error(lError);
			}
		}
	}
	
	private PriceListener createBankPriceListener(){
		return (String symbol, double price) -> {
			receiveBankPrice(symbol, price);
			logger.debug(String.format("BankPriceListener: Bank[%s] vs Company[%s]", getBankMessage(symbol), getCompanyMessage(symbol)));
		};
	}
	
	private PriceListener createCompanyPriceListener(){
		return (String symbol, double price) -> {
			receiveCompanyPrice(symbol, price);
			logger.debug(String.format("CompanyPriceListener: Bank[%s] vs Company[%s]", getBankMessage(symbol), getCompanyMessage(symbol)));
		};
	}
	
	private Runnable createCheckBankPriceTask(String pSymbol, long pSequence){
		Runnable lTask = () -> {
			Message lBankMessage = getBankMessage(pSymbol);
			if(lBankMessage.getSequence() != pSequence){
				return;
			}
			
			Message lCompanyMessage = getCompanyMessage(pSymbol);
			if(lCompanyMessage == null){
				String lError = String.format(COMPANY_UPDATE_IS_SLOW + " " + BANK_VS_COMPANY, lBankMessage, lCompanyMessage);
				alertService.alert(lError);
				logger.error(lError);
				return;
			}
			
			if(lBankMessage.getSequence() > lCompanyMessage.getSequence()){
				long lThrottle = System.currentTimeMillis() - lBankMessage.getTime();
				if(lThrottle > throttle){
					String lError = String.format(COMPANY_UPDATE_IS_SLOW + " " + BANK_VS_COMPANY, lBankMessage, lCompanyMessage);
					alertService.alert(lError);
					logger.error(lError);
					return;
				}
			}			
		};
		return lTask;
	}
	
	private Runnable createCheckCompanyPriceTask(String pSymbol, long pSequence){
		Runnable lTask = () -> {
			Message lCompanyMessage = getCompanyMessage(pSymbol);
			if(lCompanyMessage.getSequence() != pSequence){
				return;
			}
			
			Message lBankMessage = getBankMessage(pSymbol);
			if(lBankMessage == null){
				String lError = String.format(COMPANY_UPDATE_IS_INVALID + " " + BANK_VS_COMPANY, lBankMessage, lCompanyMessage);
				alertService.alert(lError);
				logger.error(lError);
				return;
			}
			
			if(lCompanyMessage.getSequence() > lBankMessage.getSequence()){
				long lThrottle = lCompanyMessage.getTime() - lBankMessage.getTime();
				if(lThrottle > throttle){
					String lError = String.format(COMPANY_UPDATE_IS_SLOW + " " + BANK_VS_COMPANY, lBankMessage, lCompanyMessage);
					alertService.alert(lError);
					logger.error(lError);
					return;
				}
				
				if(lCompanyMessage.getPrice() != lBankMessage.getPrice()){
					String lError = String.format(COMPANY_UPDATE_IS_INVALID + " " + BANK_VS_COMPANY, lBankMessage, lCompanyMessage);
					alertService.alert(lError);
					logger.error(lError);
					return;
				}
			}
		};
		return lTask;
	}
	
	private Message getBankMessage(String pSymbol){
		return bankMessages.get(pSymbol);
	}
	
	private Message getCompanyMessage(String pSymbol){
		return companyMessages.get(pSymbol);
	}
	
}

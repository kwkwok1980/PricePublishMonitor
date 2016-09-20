package com.morgan.stanley;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.morgan.stanley.AlertService;
import com.morgan.stanley.PricePublishMonitor;
import com.morgan.stanley.PriceUpdateService;

import static org.mockito.Mockito.*;

public class PricePublishMonitorTest {

	@Test
	public void testCompanyUpdateCorrect() throws Exception{
		PriceUpdateService lPriceUpdateService = Mockito.mock(PriceUpdateService.class);
		AlertService lAlertService = Mockito.mock(AlertService.class);
		long lThrottle = 5000;
		
		PricePublishMonitor lPricePublishMonitor = new PricePublishMonitor(lPriceUpdateService, lAlertService, lThrottle);
		lPricePublishMonitor.start();
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 91.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 92.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 93.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.stop();
		verify(lAlertService, times(0)).alert(Matchers.anyString());
	}
	
	@Test
	public void testCompanyUpdateWrongPrice() throws Exception{
		PriceUpdateService lPriceUpdateService = Mockito.mock(PriceUpdateService.class);
		AlertService lAlertService = Mockito.mock(AlertService.class);
		long lThrottle = 5000;
		
		PricePublishMonitor lPricePublishMonitor = new PricePublishMonitor(lPriceUpdateService, lAlertService, lThrottle);
		lPricePublishMonitor.start();
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 91.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 92.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 93.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("1.HK", 95.0);
		lPricePublishMonitor.stop();
		
		verify(lAlertService, times(1)).alert(Matchers.anyString());
		verify(lAlertService, times(1)).alert(Matchers.startsWith(PricePublishMonitor.COMPANY_UPDATE_IS_INVALID));
	}
	
	@Test
	public void testBankSendNoUpdate() throws Exception{
		PriceUpdateService lPriceUpdateService = Mockito.mock(PriceUpdateService.class);
		AlertService lAlertService = Mockito.mock(AlertService.class);
		long lThrottle = 5000;
		
		PricePublishMonitor lPricePublishMonitor = new PricePublishMonitor(lPriceUpdateService, lAlertService, lThrottle);
		lPricePublishMonitor.start();
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("1.HK", 95.0);
		lPricePublishMonitor.stop();
		
		verify(lAlertService, times(1)).alert(Matchers.anyString());
		verify(lAlertService, times(1)).alert(Matchers.startsWith(PricePublishMonitor.COMPANY_UPDATE_IS_INVALID));
	}
	
	@Test
	public void testCompanySendNoUpdate() throws Exception{
		PriceUpdateService lPriceUpdateService = Mockito.mock(PriceUpdateService.class);
		AlertService lAlertService = Mockito.mock(AlertService.class);
		long lThrottle = 5000;
		
		PricePublishMonitor lPricePublishMonitor = new PricePublishMonitor(lPriceUpdateService, lAlertService, lThrottle);
		lPricePublishMonitor.start();
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 91.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 92.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 93.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.stop();
		
		verify(lAlertService, times(1)).alert(Matchers.anyString());
		verify(lAlertService, times(1)).alert(Matchers.startsWith(PricePublishMonitor.COMPANY_UPDATE_IS_SLOW));
	}
	
	@Test
	public void testCompanyUpdateAfterThrottle() throws Exception{
		PriceUpdateService lPriceUpdateService = Mockito.mock(PriceUpdateService.class);
		AlertService lAlertService = Mockito.mock(AlertService.class);
		long lThrottle = 5000;
		
		PricePublishMonitor lPricePublishMonitor = new PricePublishMonitor(lPriceUpdateService, lAlertService, lThrottle);
		lPricePublishMonitor.start();
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 91.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 92.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 93.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("1.HK", 94.0);
		
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 95.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 96.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 97.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 98.0);
		Thread.sleep(lThrottle + 1000);
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("1.HK", 98.0);
		lPricePublishMonitor.stop();
		
		verify(lAlertService, times(2)).alert(Matchers.anyString());
		verify(lAlertService, times(2)).alert(Matchers.startsWith(PricePublishMonitor.COMPANY_UPDATE_IS_SLOW));
	}
	
	@Test
	public void testCompanySendDuplicateUpdate() throws Exception{
		PriceUpdateService lPriceUpdateService = Mockito.mock(PriceUpdateService.class);
		AlertService lAlertService = Mockito.mock(AlertService.class);
		long lThrottle = 5000;
		
		PricePublishMonitor lPricePublishMonitor = new PricePublishMonitor(lPriceUpdateService, lAlertService, lThrottle);
		lPricePublishMonitor.start();
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 91.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 92.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 93.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("1.HK", 94.0);
		Thread.sleep(lThrottle + 1000);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.stop();
		
		verify(lAlertService, times(1)).alert(Matchers.anyString());
		verify(lAlertService, times(1)).alert(Matchers.startsWith(PricePublishMonitor.COMPANY_UPDATE_IS_INVALID));
	}
	
	@Test
	public void testHandleMultipleAssets() throws Exception{
		PriceUpdateService lPriceUpdateService = Mockito.mock(PriceUpdateService.class);
		AlertService lAlertService = Mockito.mock(AlertService.class);
		long lThrottle = 5000;
		
		PricePublishMonitor lPricePublishMonitor = new PricePublishMonitor(lPriceUpdateService, lAlertService, lThrottle);
		lPricePublishMonitor.start();
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 91.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("2.HK", 91.0);
		
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 92.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("2.HK", 92.0);
		
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 93.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("2.HK", 93.0);
		
		lPricePublishMonitor.getBankPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.getBankPriceListener().priceUpdate("2.HK", 94.0);
		
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("1.HK", 94.0);
		lPricePublishMonitor.getCompanyPriceListener().priceUpdate("2.HK", 94.0);
		
		lPricePublishMonitor.stop();
		verify(lAlertService, times(0)).alert(Matchers.anyString());
	}
	
	
	
		
	
}

package com.morgan.stanley;

public class Message {
	
	private String symbol;
	private double price;
	private long time;
	private long sequence;
	
	public Message(String pSymbol, double pPrice, long time, long sequence){
		this.symbol = pSymbol;
		this.price = pPrice;
		this.time = time;
		this.sequence = sequence;
	}
	
	public String getSymbol() {
		return symbol;
	}

	public double getPrice() {
		return price;
	}

	public long getTime() {
		return time;
	}
	
	public long getSequence(){
		return sequence;
	}
	
	@Override
	public String toString(){
		return String.format("sequence[%d], time[%d], symbol[%s], price[%f]", sequence, time, symbol, price);
	}
	
}

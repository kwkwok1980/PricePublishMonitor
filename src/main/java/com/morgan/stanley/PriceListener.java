package com.morgan.stanley;

public interface PriceListener {
	
	/**
     * This method gets called each time a price update is sent.
     * 
     * @param symbol the product symbol
     * @param price the product price
     */
    void priceUpdate(String symbol, double price);

}

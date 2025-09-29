package service;

import model.Transaction;

public class BitcoinFeeCalculator implements FeeCalculator {
    private int sizeBytes;
    private double satoshiPerByte;

    public BitcoinFeeCalculator(int sizeBytes, double satoshiPerByte){
        this.sizeBytes = sizeBytes;
        this.satoshiPerByte = satoshiPerByte;
    }

    public int getSizeBytes(){ return sizeBytes; }
    public double getSatoshiPerByte(){ return satoshiPerByte; }

    @Override
    public double calculateFee(Transaction tx){
        return sizeBytes * satoshiPerByte;
    } 
}

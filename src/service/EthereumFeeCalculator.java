package service;

import model.Transaction;

public class EthereumFeeCalculator implements FeeCalculator {
    private int gasLimit;
    private double gasPrice;

    public EthereumFeeCalculator(int gasLimit, double gasPrice){
        this.gasLimit = gasLimit;
        this.gasPrice = gasPrice;
    }

    public int getGasLimit(){ return gasLimit; }
    public double getGasPrice(){ return gasPrice; }

    @Override
    public double calculateFee(Transaction tx){
        return gasPrice * gasLimit;
    }
}

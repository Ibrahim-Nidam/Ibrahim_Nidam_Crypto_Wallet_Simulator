package service;

import model.Transaction;

public interface FeeCalculator {
    double calculateFee(Transaction tx);
}

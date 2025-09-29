package model;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import model.enums.CryptoType;
import model.enums.FeeLevel;

public class Wallet {
    private UUID id;
    private String address;
    private String password;
    private double balance;
    private CryptoType type;
    private List<Transaction> transactions;
    
    // Constructor for new wallets
    public Wallet(String address, String password, CryptoType type){
        this.id = UUID.randomUUID();
        this.address = address;
        this.password = password;
        this.balance = 0;
        this.type = type;
        this.transactions = new ArrayList<>();
    }
    
    // Constructor for existing wallets from database
    public Wallet(UUID id, String address, String password, CryptoType type, double balance){
        this.id = id;
        this.address = address;
        this.password = password;
        this.balance = balance;
        this.type = type;
        this.transactions = new ArrayList<>();
    }
    
    public Transaction createTransaction(String destination, double amount, FeeLevel level){
        if(destination == null || destination.isEmpty() || amount <= 0 || level == null){
            throw new IllegalArgumentException("Paramètres invalides pour la transaction");
        }
        Transaction tx = new Transaction(this.address, destination, amount, level);
        tx.setWalletId(this.id);
        this.transactions.add(tx);
        return tx;
    }
    
    public boolean verifyPassword(String input){
        return this.password.equals(input);
    }
    
    public List<Transaction> getTransactions(){
        return transactions;
    }
    
    public UUID getId(){ return id; }
    public String getAddress(){ return address; }
    public double getBalance(){ return balance; }
    public String getPassword(){ return password; }
    public CryptoType getType(){ return type; }
    
    public void SetBalance(double amount){
        if(this.balance < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance += amount;
    }
}
package model;

import java.time.LocalDateTime;
import java.util.UUID;
import model.enums.FeeLevel;
import model.enums.TransactionStatus;

public class Transaction {
    private UUID id;
    private UUID walletId;
    private String sourceAddress;
    private String destinationAddress;
    private double amount;
    private double fee;
    private FeeLevel feeLevel;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private boolean isMock;

    // Constructor for creating new transactions
    public Transaction(String sourceAddress, String destinationAddress, double amount, FeeLevel feeLevel) {
        this.id = UUID.randomUUID();
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.amount = amount;
        this.feeLevel = feeLevel;
        this.status = TransactionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.isMock = false;
    }

    // Constructor for database retrieval
    public Transaction(UUID id, UUID walletId, String sourceAddress, String destinationAddress,
                    double amount, double fee, FeeLevel feeLevel, TransactionStatus status,
                    LocalDateTime createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.amount = amount;
        this.fee = fee;
        this.feeLevel = feeLevel;
        this.status = status;
        this.createdAt = createdAt;
        this.isMock = false;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public String getSourceAddress() { return sourceAddress; }
    public String getDestinationAddress() { return destinationAddress; }
    public double getAmount() { return amount; }
    public double getFee() { return fee; }
    public FeeLevel getFeeLevel() { return feeLevel; }
    public TransactionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isMock() { return isMock; }
    
    public void setId(UUID id) { this.id = id; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public void setSourceAddress(String sourceAddress) { this.sourceAddress = sourceAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setFee(double fee) { this.fee = fee; }
    public void setFeeLevel(FeeLevel feeLevel) { this.feeLevel = feeLevel; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setMock(boolean isMock) { this.isMock = isMock; }
    
    @Override
    public String toString() {
        String truncatedSource = sourceAddress != null && sourceAddress.length() > 8 
                                ? sourceAddress.substring(0, 8) + "..." 
                                : sourceAddress != null ? sourceAddress : "null";
                                
        String truncatedDestination = destinationAddress != null && destinationAddress.length() > 8 
                                    ? destinationAddress.substring(0, 8) + "..." 
                                    : destinationAddress != null ? destinationAddress : "null";
        
        return String.format("Transaction{id=%s, from=%s, to=%s, amount=%.6f, fee=%.6f, status=%s}", 
                        id.toString().substring(0, 8) + "...", 
                        truncatedSource, truncatedDestination, amount, fee, status);
    }
}
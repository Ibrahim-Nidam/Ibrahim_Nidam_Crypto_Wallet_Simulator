package model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.enums.FeeLevel;
import model.enums.TransactionStatus;

public class Mempool {
    private List<Transaction> pendingTx = new ArrayList<>();
    private Random random = new Random();

    public boolean addTransactionIfNotExists(Transaction transaction) {
        if (pendingTx.stream().noneMatch(tx -> tx.getId().equals(transaction.getId()))) {
            pendingTx.add(transaction);
            sortTransactions();
            return true;
        }
        return false;
    }

    public int getPosition(Transaction transaction) {
        for (int i = 0; i < pendingTx.size(); i++) {
            if (pendingTx.get(i).getId().equals(transaction.getId())) {
                return i + 1;
            }
        }
        return -1;
    }

    public Duration estimateTime(Transaction transaction) {
        int position = getPosition(transaction);
        if (position == -1) {
            return Duration.ofMinutes(0);
        }
        return Duration.ofMinutes(position * 10L);
    }

    public void sortTransactions() {
        pendingTx.sort((t1, t2) -> Double.compare(t2.getFee(), t1.getFee()));
    }

    public List<Transaction> getPendingTx() {
        return pendingTx.stream()
            .filter(tx -> tx.getStatus() == TransactionStatus.PENDING)
            .collect(java.util.stream.Collectors.toList());
    }

    public void cleanupProcessedTransactions() {
        pendingTx.removeIf(tx -> tx.getStatus() != TransactionStatus.PENDING);
    }
    public List<Transaction> getActualPendingList() {
        return pendingTx; 
    }

    public void clearMempool() {
        pendingTx.clear();
    }

    public void generateRandomTransactions(int count) {
        String[] prefixes = {"1", "3", "bc1", "0x"};
        for (int i = 0; i < count; i++) {
            String source = generateRandomAddress(prefixes[random.nextInt(prefixes.length)]);
            String destination = generateRandomAddress(prefixes[random.nextInt(prefixes.length)]);
            double amount = 0.1 + random.nextDouble() * 9.9;
            double fee = 0.0001 + random.nextDouble() * 0.0099;
            FeeLevel[] levels = FeeLevel.values();
            FeeLevel feeLevel = levels[random.nextInt(levels.length)];
            Transaction tx = new Transaction(source, destination, amount, feeLevel);
            tx.setFee(fee);
            tx.setMock(true);
            addTransactionIfNotExists(tx);
        }
    }

    public int size() {
        return pendingTx.size();
    }

    private String generateRandomAddress(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        String chars = prefix.startsWith("0x") ? "0123456789abcdef" : "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        int length = prefix.startsWith("0x") ? 40 : (prefix.equals("bc1") ? 39 : 26);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
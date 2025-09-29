package service;

import model.Mempool;
import model.Transaction;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

public class MempoolService {
    private static final Logger logger = Logger.getLogger(MempoolService.class.getName());
    private final Mempool mempool;

    public MempoolService(Mempool mempool) {
        this.mempool = mempool;
    }

    public void addTransaction(Transaction transaction) {
        if (!mempool.addTransactionIfNotExists(transaction)) {
            logger.warning("Transaction " + transaction.getId() + " already exists in mempool");
        }
    }

    public void removeTransaction(Transaction transaction) {
        boolean removed = false;
        
        List<Transaction> actualList = mempool.getActualPendingList();
        removed = actualList.removeIf(tx -> tx.getId().equals(transaction.getId()));
        
        if (!removed) {
            logger.warning("Transaction " + transaction.getId() + " was not found in mempool for removal");
        } else {
            logger.info("Transaction " + transaction.getId() + " removed from mempool");
        }
    }

    public void cleanupProcessedTransactions() {
        mempool.cleanupProcessedTransactions();
    }

    public void sortTransactions() {
        mempool.getPendingTx().sort((t1, t2) -> Double.compare(t2.getFee(), t1.getFee()));
    }

    public int getPosition(Transaction transaction) {
        return mempool.getPosition(transaction);
    }

    public Duration estimateTime(Transaction transaction) {
        return mempool.estimateTime(transaction);
    }

    public List<Transaction> getPendingTransactions() {
        return mempool.getPendingTx();
    }

    public void loadPendingTransactions(List<Transaction> transactions) {
        mempool.clearMempool();
        for (Transaction tx : transactions) {
            addTransaction(tx);
        }
    }

    public void generateRandomTransactions(int count) {
        mempool.generateRandomTransactions(count);
    }

    public int size() {
        return mempool.size();
    }
}
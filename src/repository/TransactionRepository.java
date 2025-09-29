package repository;

import model.Transaction;
import model.enums.FeeLevel;
import model.enums.TransactionStatus;
import utils.DBConnection;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class TransactionRepository {
    private static final Logger logger = Logger.getLogger(TransactionRepository.class.getName());
    
    // Constants for column names
    private static final String ID_COLUMN = "id";
    private static final String WALLET_ID_COLUMN = "wallet_id";
    private static final String SOURCE_ADDRESS_COLUMN = "source_address";
    private static final String DESTINATION_ADDRESS_COLUMN = "destination_address";
    private static final String AMOUNT_COLUMN = "amount";
    private static final String FEE_COLUMN = "fee";
    private static final String FEE_LEVEL_COLUMN = "fee_level";
    private static final String STATUS_COLUMN = "status";
    private static final String CREATED_AT_COLUMN = "created_at";

    // Custom exception for repository errors
    public static class TransactionRepositoryException extends RuntimeException {
        public TransactionRepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public void save(Transaction tx) {
        String sql = String.format(
            "INSERT INTO transactions (%s, %s, %s, %s, %s, %s, %s, %s, %s) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (%s) DO UPDATE SET " +
            "%s = EXCLUDED.%s, " +
            "%s = EXCLUDED.%s, " +
            "%s = EXCLUDED.%s, " +
            "%s = EXCLUDED.%s, " +
            "%s = EXCLUDED.%s, " +
            "%s = EXCLUDED.%s, " +
            "%s = EXCLUDED.%s",
            ID_COLUMN, WALLET_ID_COLUMN, SOURCE_ADDRESS_COLUMN, DESTINATION_ADDRESS_COLUMN,
            AMOUNT_COLUMN, FEE_COLUMN, FEE_LEVEL_COLUMN, STATUS_COLUMN, CREATED_AT_COLUMN,
            ID_COLUMN,
            WALLET_ID_COLUMN, WALLET_ID_COLUMN,
            SOURCE_ADDRESS_COLUMN, SOURCE_ADDRESS_COLUMN,
            DESTINATION_ADDRESS_COLUMN, DESTINATION_ADDRESS_COLUMN,
            AMOUNT_COLUMN, AMOUNT_COLUMN,
            FEE_COLUMN, FEE_COLUMN,
            FEE_LEVEL_COLUMN, FEE_LEVEL_COLUMN,
            STATUS_COLUMN, STATUS_COLUMN
        );
                    
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Validate transaction data before saving
            if (tx.getId() == null) {
                throw new IllegalArgumentException("Transaction ID cannot be null");
            }
            if (tx.getWalletId() == null) {
                throw new IllegalArgumentException("Wallet ID cannot be null");
            }
            
            stmt.setObject(1, tx.getId());
            stmt.setObject(2, tx.getWalletId());
            stmt.setString(3, tx.getSourceAddress());
            stmt.setString(4, tx.getDestinationAddress());
            stmt.setDouble(5, tx.getAmount());
            stmt.setDouble(6, tx.getFee());
            stmt.setString(7, tx.getFeeLevel() != null ? tx.getFeeLevel().name() : FeeLevel.STANDARD.name());
            stmt.setString(8, tx.getStatus().name());
            stmt.setTimestamp(9, Timestamp.valueOf(tx.getCreatedAt()));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            String errorMsg = String.format("SQL error saving transaction %s: %s (SQL State: %s, Error Code: %d)",
                tx.getId(), e.getMessage(), e.getSQLState(), e.getErrorCode());
            logger.severe(errorMsg);
            throw new TransactionRepositoryException(errorMsg, e);
        } catch (IllegalArgumentException e) {
            String errorMsg = String.format("Validation error saving transaction %s: %s",
                tx.getId(), e.getMessage());
            logger.severe(errorMsg);
            throw new TransactionRepositoryException(errorMsg, e);
        }
    }
    
    public List<Transaction> findByWalletId(UUID walletId) {
        List<Transaction> result = new ArrayList<>();
        String sql = String.format(
            "SELECT %s, %s, %s, %s, %s, %s, %s, %s, %s " +
            "FROM transactions WHERE %s = ? ORDER BY %s DESC",
            ID_COLUMN, WALLET_ID_COLUMN, SOURCE_ADDRESS_COLUMN, DESTINATION_ADDRESS_COLUMN,
            AMOUNT_COLUMN, FEE_COLUMN, FEE_LEVEL_COLUMN, STATUS_COLUMN, CREATED_AT_COLUMN,
            WALLET_ID_COLUMN, CREATED_AT_COLUMN
        );
                    
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, walletId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction tx = new Transaction(
                        (UUID) rs.getObject(ID_COLUMN),
                        (UUID) rs.getObject(WALLET_ID_COLUMN),
                        rs.getString(SOURCE_ADDRESS_COLUMN),
                        rs.getString(DESTINATION_ADDRESS_COLUMN),
                        rs.getDouble(AMOUNT_COLUMN),
                        rs.getDouble(FEE_COLUMN),
                        FeeLevel.valueOf(rs.getString(FEE_LEVEL_COLUMN)),
                        TransactionStatus.valueOf(rs.getString(STATUS_COLUMN)),
                        rs.getTimestamp(CREATED_AT_COLUMN).toLocalDateTime()
                    );
                    result.add(tx);
                }
            }
        } catch (SQLException e) {
            String errorMsg = String.format("Error retrieving transactions for wallet %s: %s",
                walletId, e.getMessage());
            logger.severe(errorMsg);
            throw new TransactionRepositoryException(errorMsg, e);
        }
        return result;
    }
    
    public Optional<Transaction> findById(UUID transactionId) {
        String sql = String.format(
            "SELECT %s, %s, %s, %s, %s, %s, %s, %s, %s " +
            "FROM transactions WHERE %s = ?",
            ID_COLUMN, WALLET_ID_COLUMN, SOURCE_ADDRESS_COLUMN, DESTINATION_ADDRESS_COLUMN,
            AMOUNT_COLUMN, FEE_COLUMN, FEE_LEVEL_COLUMN, STATUS_COLUMN, CREATED_AT_COLUMN,
            ID_COLUMN
        );
                    
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, transactionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Transaction tx = new Transaction(
                        (UUID) rs.getObject(ID_COLUMN),
                        (UUID) rs.getObject(WALLET_ID_COLUMN),
                        rs.getString(SOURCE_ADDRESS_COLUMN),
                        rs.getString(DESTINATION_ADDRESS_COLUMN),
                        rs.getDouble(AMOUNT_COLUMN),
                        rs.getDouble(FEE_COLUMN),
                        FeeLevel.valueOf(rs.getString(FEE_LEVEL_COLUMN)),
                        TransactionStatus.valueOf(rs.getString(STATUS_COLUMN)),
                        rs.getTimestamp(CREATED_AT_COLUMN).toLocalDateTime()
                    );
                    return Optional.of(tx);
                }
            }
        } catch (SQLException e) {
            String errorMsg = String.format("Error finding transaction %s: %s",
                transactionId, e.getMessage());
            logger.severe(errorMsg);
            throw new TransactionRepositoryException(errorMsg, e);
        }
        return Optional.empty();
    }

    public List<Transaction> findAllPending() {
        List<Transaction> result = new ArrayList<>();
        String sql = String.format(
            "SELECT %s, %s, %s, %s, %s, %s, %s, %s, %s " +
            "FROM transactions WHERE %s = 'PENDING' ORDER BY %s DESC, %s ASC",
            ID_COLUMN, WALLET_ID_COLUMN, SOURCE_ADDRESS_COLUMN, DESTINATION_ADDRESS_COLUMN,
            AMOUNT_COLUMN, FEE_COLUMN, FEE_LEVEL_COLUMN, STATUS_COLUMN, CREATED_AT_COLUMN,
            STATUS_COLUMN, FEE_COLUMN, CREATED_AT_COLUMN
        );
                    
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction tx = new Transaction(
                        (UUID) rs.getObject(ID_COLUMN),
                        (UUID) rs.getObject(WALLET_ID_COLUMN),
                        rs.getString(SOURCE_ADDRESS_COLUMN),
                        rs.getString(DESTINATION_ADDRESS_COLUMN),
                        rs.getDouble(AMOUNT_COLUMN),
                        rs.getDouble(FEE_COLUMN),
                        FeeLevel.valueOf(rs.getString(FEE_LEVEL_COLUMN)),
                        TransactionStatus.valueOf(rs.getString(STATUS_COLUMN)),
                        rs.getTimestamp(CREATED_AT_COLUMN).toLocalDateTime()
                    );
                    result.add(tx);
                }
            }
        } catch (SQLException e) {
            String errorMsg = "Error retrieving all pending transactions: " + e.getMessage();
            logger.severe(errorMsg);
            throw new TransactionRepositoryException(errorMsg, e);
        }
        return result;
    }
}
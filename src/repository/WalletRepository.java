package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import model.Wallet;
import model.enums.CryptoType;
import utils.DBConnection;

public class WalletRepository {
    
    private static final Logger logger = Logger.getLogger(WalletRepository.class.getName());
    
    public void save(Wallet wallet){
        String sql = "INSERT INTO wallets (id, type, address, password, balance) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO UPDATE SET " +
                "type = EXCLUDED.type, " +
                "address = EXCLUDED.address, " +
                "password = EXCLUDED.password, " +
                "balance = EXCLUDED.balance";
        try{
            Connection conn = DBConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, wallet.getId());
                stmt.setString(2, wallet.getType().name());
                stmt.setString(3, wallet.getAddress());
                stmt.setString(4, wallet.getPassword());
                stmt.setDouble(5, wallet.getBalance());
                stmt.executeUpdate();
            }
        } catch(SQLException e){
            logger.severe("Error saving wallet : " + e.getMessage());
            throw new RuntimeException("Failed to save wallet", e);
        }
    }
    
    public Optional<Wallet> findById(UUID id){
        String sql = "SELECT id, type, address, password, balance FROM wallets WHERE id = ?";
        try (Connection conn = DBConnection.getInstance();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if(rs.next()){
                    Wallet wallet = createWalletFromResultSet(rs);
                    return Optional.of(wallet);
                }
            }
        } catch (SQLException e) {
            logger.severe("Error finding wallet by id: " + e.getMessage());
            throw new RuntimeException("Failed to find wallet", e);
        }
        return Optional.empty();
    }
    
    public List<Wallet> findAll(){
        List<Wallet> wallets = new ArrayList<>();
        String sql = "SELECT id, type, address, password, balance FROM wallets ORDER BY id";
        try{
            Connection conn = DBConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
                while(rs.next()){
                    Wallet wallet = createWalletFromResultSet(rs);
                    wallets.add(wallet);
                }
            }
        } catch (Exception e) {
            logger.severe("Error finding all wallets: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve wallets", e);
        }
        return wallets;
    }
    
    private Wallet createWalletFromResultSet(ResultSet rs) throws SQLException{
        UUID id = (UUID) rs.getObject("id");
        CryptoType type = CryptoType.valueOf(rs.getString("type"));
        String address = rs.getString("address");
        String password = rs.getString("password");
        double balance = rs.getDouble("balance");
        
        Wallet wallet = new Wallet(id, address, password, type, balance);
        return wallet;
    }
}
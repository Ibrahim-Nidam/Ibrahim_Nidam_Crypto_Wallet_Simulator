package service;

import model.Wallet;
import model.enums.CryptoType;
import repository.WalletRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.logging.Logger;

public class WalletService {
    private static final Logger logger = Logger.getLogger(WalletService.class.getName());
    private final WalletRepository walletRepository;
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final String BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet createWallet(CryptoType type, String password) {
        String address = generateAddress(type);
        Wallet wallet = new Wallet(address, password, type);
        walletRepository.save(wallet);
        return wallet;
    }

    public void rechargeWallet(Wallet wallet, double amount) {
        try {
            wallet.SetBalance(amount);
            walletRepository.save(wallet);
        } catch (Exception e) {
            logger.severe("Error recharging wallet: " + e.getMessage());
            throw new RuntimeException("Failed to recharge wallet", e);
        }
    }

    public void debitWallet(Wallet wallet, double amount) {
        try {
            wallet.SetBalance(-amount);
            walletRepository.save(wallet);
        } catch (Exception e) {
            logger.severe("Error debiting wallet: " + e.getMessage());
            throw new RuntimeException("Failed to debit wallet", e);
        }
    }

    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    private String generateAddress(CryptoType type) {
        if (type == CryptoType.BITCOIN) {
            String[] prefixes = {"1", "3", "bc1"};
            String prefix = prefixes[random.nextInt(prefixes.length)];
            StringBuilder sb = new StringBuilder(prefix);
            int length = prefix.equals("bc1") ? 39 : 26;
            for (int i = 0; i < length; i++) {
                sb.append(BASE58_CHARS.charAt(random.nextInt(BASE58_CHARS.length())));
            }
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder("0x");
            for (int i = 0; i < 40; i++) {
                sb.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
            }
            return sb.toString();
        }
    }
}
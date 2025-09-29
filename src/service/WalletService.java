package service;
import java.security.SecureRandom;

public class WalletService {
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final String BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

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
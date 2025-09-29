package ui;

import java.util.Scanner;
import java.util.logging.Logger;
import model.Wallet;
import model.enums.FeeLevel;
import service.WalletService;
import service.TransactionService;

public class WalletMenu {
    private static final Logger logger = Logger.getLogger(WalletMenu.class.getName());
    private final Wallet wallet;
    private final Scanner scanner = new Scanner(System.in);
    private final WalletService walletService;
    private final TransactionService transactionService;

    public WalletMenu(Wallet wallet, WalletService walletService, TransactionService transactionService) {
        this.wallet = wallet;
        this.walletService = walletService;
        this.transactionService = transactionService;
    }

    public void showMenu() {
        int choice = 0;
        do {
            try {
                System.out.println("\n=== WALLET MENU ===");
                System.out.println("1. Créer une transaction");
                System.out.println("2. Voir ma position dans le mempool");
                System.out.println("3. Voir état du mempool");
                System.out.println("4. Recharger mon wallet");
                System.out.println("5. Retour");
                System.out.print("Choix: ");
                choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        createTransaction();
                        break;
                    case 2:
                        showPosition();
                        break;
                    case 3:
                        showMempool();
                        break;
                    case 4:
                        rechargeWallet();
                        break;
                    case 5:
                        System.out.println("Retour au menu principal.");
                        break;
                    default:
                        System.out.println("Choix invalide!");
                }
            } catch (Exception e) {
                System.out.println("Entrée invalide. Veuillez saisir un nombre.");
                scanner.nextLine();
                choice = 0;
            }
        } while (choice != 5);
    }

    private void createTransaction() {
        try {
            System.out.print("Adresse destination: ");
            String dest = scanner.nextLine();
            System.out.print("Montant (" + wallet.getType() + "): ");
            double amount = scanner.nextDouble();
            scanner.nextLine();
            System.out.print("Niveau de fees (1=ECONOMIQUE, 2=STANDARD, 3=RAPIDE): ");
            int level = scanner.nextInt();
            scanner.nextLine();
            
            FeeLevel feeLevel;
            switch (level) {
                case 1:
                    feeLevel = FeeLevel.ECONOMIQUE;
                    break;
                case 2:
                    feeLevel = FeeLevel.STANDARD;
                    break;
                case 3:
                    feeLevel = FeeLevel.RAPID;
                    break;
                default:
                    System.out.println("Niveau invalide. STANDARD sélectionné par défaut.");
                    feeLevel = FeeLevel.STANDARD;
                    break;
            }

            transactionService.createAndProcessTransaction(wallet, dest, amount, feeLevel, scanner);
        } catch (Exception e) {
            System.out.println("Erreur lors de la création de la transaction: " + e.getMessage());
            logger.severe("Transaction creation error: " + e.getMessage());
        }
    }

    private void showPosition() {
        transactionService.displayWalletPosition(wallet);
    }

    private void showMempool() {
        transactionService.displayMempoolState();
    }

    private void rechargeWallet() {
        System.out.print("Montant à déposer: ");
        double deposit = scanner.nextDouble();
        scanner.nextLine();
        try {
            walletService.rechargeWallet(wallet, deposit);
            System.out.printf("✓ Nouveau solde: %.6f %s%n", wallet.getBalance(), wallet.getType());
        } catch (Exception e) {
            System.out.println("Erreur: " + e.getMessage());
            logger.severe("Error recharging wallet: " + e.getMessage());
        }
    }
}
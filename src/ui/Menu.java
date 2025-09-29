package ui;

import java.util.Scanner;
import java.util.List;
import java.util.logging.Logger;

import model.Transaction;
import model.Wallet;
import model.enums.CryptoType;
import service.WalletService;
import service.TransactionService;
import service.MempoolService;
import utils.DBConnection;

public class Menu {
    private static final Logger logger = Logger.getLogger(Menu.class.getName());
    private final Scanner scanner = new Scanner(System.in);
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final MempoolService mempoolService;

    public Menu(WalletService walletService, TransactionService transactionService, MempoolService mempoolService) {
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.mempoolService = mempoolService;
    }

    public void start() {
        try {
            DBConnection.getInstance();
            System.out.println("✓ Connexion à la base de données établie");
            transactionService.loadAllPendingTransactions();
            if (mempoolService.size() < 10) {
                mempoolService.generateRandomTransactions(10 - mempoolService.size());
            }
        } catch (Exception e) {
            System.out.println("✗ Erreur de connexion à la base de données: " + e.getMessage());
            System.out.println("Veuillez vérifier que PostgreSQL est démarré et que la base 'crypto_wallet' existe.");
            logger.severe("Database connection failed: " + e.getMessage());
            return;
        }

        int choice = 0;
        do {
            System.out.println("\n=== CRYPTO WALLET SIMULATOR ===");
            System.out.println("1. Créer un nouveau wallet");
            System.out.println("2. Sélectionner un wallet existant");
            System.out.println("3. Lister tous les wallets");
            System.out.println("4. Mode Mineur");
            System.out.println("5. Quitter");
            System.out.print("Choix: ");

            try {
                choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:
                        createWallet();
                        break;
                    case 2:
                        selectWallet();
                        break;
                    case 3:
                        listWallets();
                        break;
                    case 4:
                        minerMode();
                        break;
                    case 5:
                        System.out.println("Au revoir !");
                        DBConnection.close();
                        break;
                    default:
                        System.out.println("Choix invalide.");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Erreur: " + e.getMessage());
                scanner.nextLine();
                logger.warning("Menu error: " + e.getMessage());
            }
        } while (choice != 5);
    }

    private void createWallet() {
        try {
            System.out.println("\n--- Création d'un wallet ---");
            System.out.println("Type de crypto:");
            System.out.println("1. BITCOIN");
            System.out.println("2. ETHEREUM");
            System.out.print("Choix: ");

            int typeChoice = scanner.nextInt();
            scanner.nextLine();

            CryptoType type = (typeChoice == 1) ? CryptoType.BITCOIN : (typeChoice == 2) ? CryptoType.ETHEREUM : CryptoType.BITCOIN;
            if (typeChoice != 1 && typeChoice != 2) {
                System.out.println("Type invalide. BITCOIN sélectionné par défaut.");
            }

            System.out.print("Mot de passe pour votre wallet: ");
            String password = scanner.nextLine();

            if (password.trim().isEmpty()) {
                System.out.println("Le mot de passe ne peut pas être vide.");
                return;
            }

            Wallet wallet = walletService.createWallet(type, password);
            System.out.println("✓ Wallet créé avec succès !");
            System.out.println("Type: " + wallet.getType());
            System.out.println("Adresse: " + wallet.getAddress());
            System.out.println("Solde initial: " + wallet.getBalance());
            logger.info("New wallet created: " + wallet.getId());
        } catch (Exception e) {
            System.out.println("Erreur lors de la création du wallet: " + e.getMessage());
            logger.severe("Failed to create wallet: " + e.getMessage());
        }
    }

    private void selectWallet() {
        try {
            System.out.println("\n--- Sélection d'un wallet ---");
            List<Wallet> wallets = walletService.getAllWallets();
            if (wallets.isEmpty()) {
                System.out.println("Aucun wallet trouvé ! Créez d'abord un wallet.");
                return;
            }

            System.out.println("Wallets disponibles:");
            for (int i = 0; i < wallets.size(); i++) {
                Wallet w = wallets.get(i);
                String shortAddr = w.getAddress().substring(0, 8) + "...";
                System.out.printf("%d. %s - %s - Solde: %.6f\n", i + 1, w.getType(), shortAddr, w.getBalance());
            }

            System.out.print("Choisissez un wallet (numéro): ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice < 1 || choice > wallets.size()) {
                System.out.println("Choix invalide.");
                return;
            }

            Wallet selected = wallets.get(choice - 1);
            System.out.print("Mot de passe: ");
            String pass = scanner.nextLine();

            if (selected.verifyPassword(pass)) {
                System.out.println("✓ Accès autorisé.");
                transactionService.loadWalletTransactions(selected);
                WalletMenu walletMenu = new WalletMenu(selected, walletService, transactionService);
                walletMenu.showMenu();
            } else {
                System.out.println("✗ Mot de passe incorrect.");
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la sélection: " + e.getMessage());
            logger.warning("Wallet selection error: " + e.getMessage());
        }
    }

    private void listWallets() {
        try {
            System.out.println("\n--- Liste des wallets ---");
            List<Wallet> wallets = walletService.getAllWallets();
            if (wallets.isEmpty()) {
                System.out.println("Aucun wallet trouvé.");
                return;
            }

            System.out.println("┌─────────────┬──────────────────┬─────────────┐");
            System.out.println("│ Type        │ Adresse          │ Solde       │");
            System.out.println("├─────────────┼──────────────────┼─────────────┤");

            for (Wallet w : wallets) {
                String shortAddr = w.getAddress().substring(0, 12) + "...";
                System.out.printf("│ %-11s │ %-16s │ %-11.6f │%n", w.getType(), shortAddr, w.getBalance());
            }

            System.out.println("└─────────────┴──────────────────┴─────────────┘");
            System.out.println("Total: " + wallets.size() + " wallet(s)");
        } catch (Exception e) {
            System.out.println("Erreur lors de la liste des wallets: " + e.getMessage());
            logger.warning("List wallets error: " + e.getMessage());
        }
    }

    private void minerMode() {
        int choice = 0;
        do {
            try {
                System.out.println("\n=== MODE MINEUR ===");
                System.out.println("1. Voir toutes les transactions en attente");
                System.out.println("2. Miner une transaction (confirmer)");
                System.out.println("3. Miner plusieurs transactions");
                System.out.println("4. Rejeter une transaction");
                System.out.println("5. Statistiques du mempool");
                System.out.println("6. Retour au menu principal");
                System.out.print("Choix: ");

                choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        transactionService.displayMempoolState();
                        break;
                    case 2:
                        mineTransaction();
                        break;
                    case 3:
                        mineMultipleTransactions();
                        break;
                    case 4:
                        rejectTransaction();
                        break;
                    case 5:
                        showMempoolStats();
                        break;
                    case 6:
                        System.out.println("Retour au menu principal.");
                        break;
                    default:
                        System.out.println("Choix invalide!");
                        break;
                }
            } catch (java.util.InputMismatchException e) {
                System.out.println("Entrée invalide. Veuillez saisir un nombre.");
                scanner.nextLine();
                choice = 0;
            }
        } while (choice != 6);
    }

    private void mineTransaction() {
        List<Transaction> pendingTx = mempoolService.getPendingTransactions();
        if (pendingTx.isEmpty()) {
            System.out.println("\nAucune transaction à miner.");
            return;
        }

        transactionService.displayMempoolState();
        System.out.print("\nNuméro de la transaction à miner (1-" + pendingTx.size() + ") ou 0 pour annuler: ");

        try {
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                System.out.println("Opération annulée.");
                return;
            }

            if (choice < 1 || choice > pendingTx.size()) {
                System.out.println("Choix invalide.");
                return;
            }

            Transaction txToMine = pendingTx.get(choice - 1);
            System.out.println("\n=== CONFIRMATION DE MINAGE ===");
            System.out.printf("Transaction ID: %s%n", txToMine.getId().toString().substring(0, 16) + "...");
            System.out.printf("De: %s%n", txToMine.getSourceAddress());
            System.out.printf("Vers: %s%n", txToMine.getDestinationAddress());
            System.out.printf("Montant: %.6f%n", txToMine.getAmount());
            System.out.printf("Frais à recevoir: %.6f%n", txToMine.getFee());
            System.out.print("\nConfirmer le minage ? (o/n): ");

            String confirm = scanner.nextLine().toLowerCase().trim();
            if (confirm.equals("o") || confirm.equals("oui")) {
                transactionService.mineTransaction(txToMine);
                System.out.printf("\n✓ Transaction minée avec succès!%n");
                System.out.printf("✓ Frais récoltés: %.6f%n", txToMine.getFee());
                logger.info("Transaction mined: " + txToMine.getId());
            } else {
                System.out.println("Minage annulé.");
            }
        } catch (java.util.InputMismatchException e) {
            System.out.println("Entrée invalide. Veuillez saisir un nombre.");
            scanner.nextLine();
        } catch (Exception e) {
            System.out.println("Erreur lors du minage: " + e.getMessage());
            logger.severe("Mining error: " + e.getMessage());
        }
    }

    private void mineMultipleTransactions() {
        List<Transaction> pendingTx = mempoolService.getPendingTransactions();
        if (pendingTx.isEmpty()) {
            System.out.println("\nAucune transaction à miner.");
            return;
        }

        transactionService.displayMempoolState();
        System.out.print("\nCombien de transactions miner (1-" + pendingTx.size() + ") ou 0 pour annuler: ");

        try {
            int count = scanner.nextInt();
            scanner.nextLine();

            if (count == 0) {
                System.out.println("Opération annulée.");
                return;
            }

            if (count < 1 || count > pendingTx.size()) {
                System.out.println("Nombre invalide.");
                return;
            }

            System.out.println("\n=== TRANSACTIONS À MINER ===");
            double previewTotalFees = 0;
            for (int i = 0; i < count; i++) {
                Transaction tx = pendingTx.get(i);
                previewTotalFees += tx.getFee();
                System.out.printf("%d. %s... - Frais: %.6f%n", i + 1, tx.getId().toString().substring(0, 8), tx.getFee());
            }
            System.out.printf("Total des frais à recevoir: %.6f%n", previewTotalFees);
            System.out.print("\nConfirmer le minage de ces " + count + " transactions ? (o/n): ");

            String confirm = scanner.nextLine().toLowerCase().trim();
            if (!confirm.equals("o") && !confirm.equals("oui")) {
                System.out.println("Minage annulé.");
                return;
            }

            transactionService.mineMultipleTransactions(count);
        } catch (java.util.InputMismatchException e) {
            System.out.println("Entrée invalide. Veuillez saisir un nombre.");
            scanner.nextLine();
        } catch (Exception e) {
            System.out.println("Erreur lors du minage multiple: " + e.getMessage());
            logger.severe("Batch mining error: " + e.getMessage());
        }
    }

    private void rejectTransaction() {
        List<Transaction> pendingTx = mempoolService.getPendingTransactions();
        if (pendingTx.isEmpty()) {
            System.out.println("\nAucune transaction à rejeter.");
            return;
        }

        transactionService.displayMempoolState();
        System.out.print("\nNuméro de la transaction à rejeter (1-" + pendingTx.size() + ") ou 0 pour annuler: ");

        try {
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                System.out.println("Opération annulée.");
                return;
            }

            if (choice < 1 || choice > pendingTx.size()) {
                System.out.println("Choix invalide.");
                return;
            }

            Transaction txToReject = pendingTx.get(choice - 1);
            System.out.println("\n=== REJET DE TRANSACTION ===");
            System.out.printf("Transaction: %s...%n", txToReject.getId().toString().substring(0, 16));
            System.out.printf("Montant: %.6f%n", txToReject.getAmount());
            System.out.printf("Frais: %.6f%n", txToReject.getFee());
            System.out.print("Raison du rejet (optionnel): ");
            String reason = scanner.nextLine().trim();
            if (reason.isEmpty()) {
                reason = "Aucune raison spécifiée";
            }

            System.out.print("\nConfirmer le rejet ? (o/n): ");
            String confirm = scanner.nextLine().toLowerCase().trim();

            if (confirm.equals("o") || confirm.equals("oui")) {
                transactionService.rejectTransaction(txToReject, reason);
                System.out.printf("\n✓ Transaction rejetée avec succès.%n");
                System.out.printf("✓ Raison: %s%n", reason);
            } else {
                System.out.println("Rejet annulé.");
            }
        } catch (java.util.InputMismatchException e) {
            System.out.println("Entrée invalide. Veuillez saisir un nombre.");
            scanner.nextLine();
        } catch (Exception e) {
            System.out.println("Erreur lors du rejet: " + e.getMessage());
            logger.severe("Rejection error: " + e.getMessage());
        }
    }

    private void showMempoolStats() {
        transactionService.displayMempoolStats();
    }
}
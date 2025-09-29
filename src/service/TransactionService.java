package service;

import model.Wallet;
import model.Transaction;
import model.enums.CryptoType;
import model.enums.FeeLevel;
import model.enums.TransactionStatus;
import repository.TransactionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class TransactionService {
    private static final Logger logger = Logger.getLogger(TransactionService.class.getName());
    private final TransactionRepository transactionRepository;
    private final MempoolService mempoolService;
    private final FeeCalculatorFactory feeCalculatorFactory;
    private final WalletService walletService;

    public TransactionService(TransactionRepository transactionRepository, MempoolService mempoolService, WalletService walletService) {
        this.transactionRepository = transactionRepository;
        this.mempoolService = mempoolService;
        this.walletService = walletService;
        this.feeCalculatorFactory = new FeeCalculatorFactory();
    }

    public void createAndProcessTransaction(Wallet wallet, String destination, double amount, FeeLevel feeLevel, Scanner scanner) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("✗ Adresse destination invalide!");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("✗ Le montant doit être positif!");
        }

        Transaction tempTx = wallet.createTransaction(destination, amount, feeLevel);
        FeeCalculator calc = feeCalculatorFactory.getFeeCalculator(wallet.getType(), feeLevel);
        double initialFee = calc.calculateFee(tempTx);
        tempTx.setFee(initialFee);

        mempoolService.addTransaction(tempTx);
        int currentPosition = mempoolService.getPosition(tempTx);
        long estimatedMinutes = mempoolService.estimateTime(tempTx).toMinutes();

        System.out.println("\n=== RÉSUMÉ DE LA TRANSACTION ===");
        System.out.printf("Montant à envoyer: %.6f %s%n", amount, wallet.getType());
        System.out.printf("Frais initiaux: %.6f %s (niveau %s)%n", initialFee, wallet.getType(), feeLevel.name());
        System.out.printf("Total à débiter: %.6f %s%n", amount + initialFee, wallet.getType());
        System.out.printf("Position actuelle dans le mempool: %d%n", currentPosition);
        System.out.printf("Temps d'attente estimé: %d minutes%n", estimatedMinutes);

        double totalCost = amount + initialFee;
        if (wallet.getBalance() < totalCost) {
            System.out.printf("✗ Solde insuffisant! Requis: %.6f %s, Disponible: %.6f %s%n",
                    totalCost, wallet.getType(), wallet.getBalance(), wallet.getType());
            mempoolService.removeTransaction(tempTx);
            wallet.getTransactions().removeIf(tx -> tx.getId().equals(tempTx.getId()));
            return;
        }

        boolean wantsToOptimize = true;
        double finalFee = initialFee;

        while (wantsToOptimize && currentPosition > 1) {
            System.out.println("\n=== OPTIMISATION DES FRAIS ===");
            System.out.println("Voulez-vous augmenter vos frais pour une meilleure position ?");
            System.out.println("1. Oui, augmenter les frais");
            System.out.println("2. Non, confirmer la transaction actuelle");
            System.out.println("3. Voir la simulation avec différents montants");
            System.out.print("Choix: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.printf("Frais actuel: %.6f %s%n", finalFee, wallet.getType());
                    System.out.print("Nouveau montant des frais (doit être > " + String.format("%.6f", finalFee) + "): ");
                    double newFee = scanner.nextDouble();
                    scanner.nextLine();

                    if (newFee <= finalFee) {
                        System.out.println("✗ Les nouveaux frais doivent être supérieurs aux frais actuels!");
                        continue;
                    }

                    double newTotalCost = amount + newFee;
                    if (wallet.getBalance() < newTotalCost) {
                        System.out.printf("✗ Solde insuffisant pour ces frais! Requis: %.6f %s, Disponible: %.6f %s%n",
                                newTotalCost, wallet.getType(), wallet.getBalance(), wallet.getType());
                        continue;
                    }

                    tempTx.setFee(newFee);
                    finalFee = newFee;
                    mempoolService.sortTransactions();
                    int newPosition = mempoolService.getPosition(tempTx);
                    long newEstimatedMinutes = mempoolService.estimateTime(tempTx).toMinutes();

                    System.out.printf("✓ Nouveaux frais appliqués: %.6f %s%n", finalFee, wallet.getType());
                    System.out.printf("Nouvelle position: %d (amélioration de %d places)%n",
                            newPosition, currentPosition - newPosition);
                    System.out.printf("Nouveau temps d'attente: %d minutes (gain de %d minutes)%n",
                            newEstimatedMinutes, estimatedMinutes - newEstimatedMinutes);

                    currentPosition = newPosition;
                    estimatedMinutes = newEstimatedMinutes;
                    totalCost = newTotalCost;
                    break;

                case 2:
                    wantsToOptimize = false;
                    break;

                case 3:
                    showFeeSimulation(tempTx, amount, wallet);
                    break;

                default:
                    System.out.println("Choix invalide!");
                    break;
            }
        }

        System.out.println("\n=== CONFIRMATION FINALE ===");
        System.out.printf("Montant: %.6f %s%n", amount, wallet.getType());
        System.out.printf("Frais: %.6f %s%n", finalFee, wallet.getType());
        System.out.printf("Total à débiter: %.6f %s%n", totalCost, wallet.getType());
        System.out.printf("Position finale: %d%n", currentPosition);
        System.out.printf("Temps d'attente estimé: %d minutes%n", estimatedMinutes);
        System.out.print("Confirmer la transaction ? (o/n): ");

        String confirm = scanner.nextLine().toLowerCase();
        if (!confirm.equals("o") && !confirm.equals("oui")) {
            System.out.println("✗ Transaction annulée.");
            mempoolService.removeTransaction(tempTx);
            wallet.getTransactions().removeIf(tx -> tx.getId().equals(tempTx.getId()));
            return;
        }

        walletService.debitWallet(wallet, totalCost);
        transactionRepository.save(tempTx);

        System.out.printf("✓ Transaction créée avec succès!%n");
        System.out.printf("  ID: %s%n", tempTx.getId().toString().substring(0, 8) + "...");
        System.out.printf("  Montant envoyé: %.6f %s%n", amount, wallet.getType());
        System.out.printf("  Frais: %.6f %s%n", finalFee, wallet.getType());
        System.out.printf("  Total débité: %.6f %s%n", totalCost, wallet.getType());
        System.out.printf("  Position dans le mempool: %d%n", currentPosition);
        System.out.printf("  Nouveau solde: %.6f %s%n", wallet.getBalance(), wallet.getType());
    }

    private void showFeeSimulation(Transaction baseTx, double amount, Wallet wallet) {
        System.out.println("\n=== SIMULATION DES FRAIS ===");
        System.out.println("┌─────────────┬─────────────┬──────────┬─────────────────┐");
        System.out.println("│ Frais       │ Position    │ Temps    │ Total à payer   │");
        System.out.println("├─────────────┼─────────────┼──────────┼─────────────────┤");

        double[] feeMultipliers = {1.0, 1.5, 2.0, 3.0, 5.0};
        double currentFee = baseTx.getFee();

        for (double multiplier : feeMultipliers) {
            double simulatedFee = currentFee * multiplier;
            Transaction simTx = new Transaction(baseTx.getSourceAddress(),
                    baseTx.getDestinationAddress(), amount, baseTx.getFeeLevel());
            simTx.setFee(simulatedFee);

            mempoolService.addTransaction(simTx);
            int position = mempoolService.getPosition(simTx);
            long minutes = mempoolService.estimateTime(simTx).toMinutes();
            double totalCost = amount + simulatedFee;

            String affordable = wallet.getBalance() >= totalCost ? "" : " (Insuffisant)";

            System.out.printf("│ %-11.6f │ %-11d │ %-8d │ %-15.6f │%s%n",
                    simulatedFee, position, minutes, totalCost, affordable);

            mempoolService.removeTransaction(simTx);
        }

        System.out.println("└─────────────┴─────────────┴──────────┴─────────────────┘");
        System.out.printf("Votre solde actuel: %.6f %s%n", wallet.getBalance(), wallet.getType());
    }

    public void displayWalletPosition(Wallet wallet) {
        List<Transaction> allTransactions = mempoolService.getPendingTransactions();
        boolean hasMyTransactions = allTransactions.stream()
                .anyMatch(tx -> tx.getSourceAddress().equals(wallet.getAddress()));

        if (!hasMyTransactions) {
            System.out.println("Aucune de vos transactions n'est actuellement dans le mempool.");
            return;
        }

        System.out.println("=== MES TRANSACTIONS DANS LE MEMPOOL ===");
        System.out.println("Transactions en attente : " + allTransactions.size());
        System.out.println("┌───────────────────────────────┬──────────┬────────┬──────────┐");
        System.out.println("│ Source -> Destination         │ Fee      │ Mine ? │ Position │");
        System.out.println("├───────────────────────────────┼──────────┼────────┼──────────┤");

        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction tx = allTransactions.get(i);
            String source = tx.getSourceAddress().length() > 6
                    ? tx.getSourceAddress().substring(0, 6) + "..."
                    : tx.getSourceAddress();
            String dest = tx.getDestinationAddress().length() > 6
                    ? tx.getDestinationAddress().substring(0, 6) + "..."
                    : tx.getDestinationAddress();
            boolean isMyTransaction = tx.getSourceAddress().equals(wallet.getAddress());
            String mineIndicator = isMyTransaction ? "  ✓   " : "      ";
            int position = i + 1;

            System.out.printf("│ %-29s │ %8.4f │ %-6s │ %-8d │%n",
                    source + " -> " + dest, tx.getFee(), mineIndicator, position);
        }

        System.out.println("└───────────────────────────────┴──────────┴────────┴──────────┘");

        System.out.println("\nRésumé de vos transactions:");
        for (Transaction tx : wallet.getTransactions()) {
            int pos = mempoolService.getPosition(tx);
            if (pos > 0) {
                System.out.printf("- Transaction %s... : Position %d, Temps estimé: %d minutes%n",
                        tx.getId().toString().substring(0, 8),
                        pos,
                        mempoolService.estimateTime(tx).toMinutes());
            }
        }
    }

    public void displayMempoolState() {
        List<Transaction> pendingTx = mempoolService.getPendingTransactions();
        System.out.println("\n=== ÉTAT DU MEMPOOL ===");
        System.out.println("Total: " + pendingTx.size() + " transaction(s)");
        System.out.println("┌────┬─────────────┬─────────────────────────────┬──────────┬──────────┬─────────────┐");
        System.out.println("│ #  │ ID          │ Source -> Destination       │ Montant  │ Frais    │ Temps       │");
        System.out.println("├────┼─────────────┼─────────────────────────────┼──────────┼──────────┬─────────────┤");

        for (int i = 0; i < pendingTx.size(); i++) {
            Transaction tx = pendingTx.get(i);
            String shortId = tx.getId().toString().substring(0, 8) + "...";
            String source = tx.getSourceAddress().length() > 6
                    ? tx.getSourceAddress().substring(0, 6) + "..."
                    : tx.getSourceAddress();
            String dest = tx.getDestinationAddress().length() > 6
                    ? tx.getDestinationAddress().substring(0, 6) + "..."
                    : tx.getDestinationAddress();
            String timeAgo = getTimeAgo(tx.getCreatedAt());

            System.out.printf("│ %-2d │ %-11s │ %-27s │ %8.4f │ %8.4f │ %-11s │%n",
                    i + 1, shortId, source + " -> " + dest, tx.getAmount(), tx.getFee(), timeAgo);
        }

        System.out.println("└────┴─────────────┴─────────────────────────────┴──────────┴──────────┴─────────────┘");
    }

    public void mineTransaction(Transaction transaction) {
        try {
            transaction.setStatus(TransactionStatus.CONFIRMED);
            
            if (!transaction.isMock() && transaction.getWalletId() != null) {
                transactionRepository.save(transaction);
            }
            
            mempoolService.removeTransaction(transaction);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to mine transaction %s: %s",
                transaction.getId(), e.getMessage());
            logger.severe(errorMsg);
            throw new TransactionRepository.TransactionRepositoryException(errorMsg, e);
        }
    }

    public void mineMultipleTransactions(int count) {
        List<Transaction> pendingTx = mempoolService.getPendingTransactions();
        double totalFees = 0;
        int successCount = 0;
        List<Transaction> transactionsToRemove = new ArrayList<>();

        System.out.println("\n=== MINAGE EN COURS ===");
        for (int i = 0; i < count && i < pendingTx.size(); i++) {
            Transaction tx = pendingTx.get(i);
            try {
                tx.setStatus(TransactionStatus.CONFIRMED);
                
                if (!tx.isMock() && tx.getWalletId() != null) {
                    transactionRepository.save(tx);
                }
                
                totalFees += tx.getFee();
                successCount++;
                transactionsToRemove.add(tx);
                System.out.printf("✓ Transaction %s... minée (frais: %.6f)%n",
                        tx.getId().toString().substring(0, 8), tx.getFee());
            } catch (Exception e) {
                System.out.printf("✗ Erreur transaction %s...: %s%n",
                        tx.getId().toString().substring(0, 8), e.getMessage());
                logger.severe("Mining error for tx " + tx.getId() + ": " + e.getMessage());
            }
        }

        for (Transaction tx : transactionsToRemove) {
            mempoolService.removeTransaction(tx);
        }

        mempoolService.cleanupProcessedTransactions();

        System.out.printf("\n=== RÉSULTAT DU MINAGE ===\n");
        System.out.printf("✓ Transactions minées avec succès: %d/%d%n", successCount, count);
        System.out.printf("✓ Total des frais récoltés: %.6f%n", totalFees);
        if (successCount < count) {
            System.out.printf("⚠ Échecs: %d transaction(s)%n", count - successCount);
        }
        logger.info("Batch mining completed: " + successCount + " transactions, total fees: " + totalFees);
    }

    public void rejectTransaction(Transaction transaction, String reason) {
        try {
            transaction.setStatus(TransactionStatus.REJECTED);
            
            if (!transaction.isMock() && transaction.getWalletId() != null) {
                transactionRepository.save(transaction);
            }
            
            mempoolService.removeTransaction(transaction);
            logger.info("Transaction rejected: " + transaction.getId() + " - Reason: " + reason);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to reject transaction %s: %s",
                transaction.getId(), e.getMessage());
            logger.severe(errorMsg);
            throw new TransactionRepository.TransactionRepositoryException(errorMsg, e);
        }
    }

    public void displayMempoolStats() {
        List<Transaction> pendingTx = mempoolService.getPendingTransactions();
        if (pendingTx.isEmpty()) {
            System.out.println("Le mempool est vide.");
            return;
        }

        double totalFees = pendingTx.stream().mapToDouble(Transaction::getFee).sum();
        double avgFee = totalFees / pendingTx.size();
        double maxFee = pendingTx.stream().mapToDouble(Transaction::getFee).max().orElse(0);
        double minFee = pendingTx.stream().mapToDouble(Transaction::getFee).min().orElse(0);

        System.out.println("\n=== STATISTIQUES DU MEMPOOL ===");
        System.out.printf("Nombre de transactions: %d%n", pendingTx.size());
        System.out.printf("Total des frais: %.6f%n", totalFees);
        System.out.printf("Frais moyen: %.6f%n", avgFee);
        System.out.printf("Frais maximum: %.6f%n", maxFee);
        System.out.printf("Frais minimum: %.6f%n", minFee);

        System.out.println("\nDistribution des frais:");
        System.out.println("┌─────────────────┬───────────┐");
        System.out.println("│ Plage de frais  │ Nombre    │");
        System.out.println("├─────────────────┼───────────┤");

        int lowFees = (int) pendingTx.stream().filter(tx -> tx.getFee() < avgFee * 0.5).count();
        int mediumFees = (int) pendingTx.stream().filter(tx -> tx.getFee() >= avgFee * 0.5 && tx.getFee() < avgFee * 1.5).count();
        int highFees = (int) pendingTx.stream().filter(tx -> tx.getFee() >= avgFee * 1.5).count();

        System.out.printf("│ Faibles         │ %-9d │%n", lowFees);
        System.out.printf("│ Moyens          │ %-9d │%n", mediumFees);
        System.out.printf("│ Élevés          │ %-9d │%n", highFees);
        System.out.println("└─────────────────┴───────────┘");
    }

    public void loadWalletTransactions(Wallet wallet) {
        try {
            List<Transaction> dbTransactions = transactionRepository.findByWalletId(wallet.getId());
            for (Transaction tx : dbTransactions) {
                if (tx.getStatus() == TransactionStatus.PENDING) {
                    wallet.getTransactions().add(tx);
                }
            }
            if (!dbTransactions.isEmpty()) {
                System.out.println("✓ " + dbTransactions.size() + " transactions chargées depuis la base de données");
            }
        } catch (Exception e) {
            logger.severe("Error loading wallet transactions: " + e.getMessage());
            throw new TransactionRepository.TransactionRepositoryException(
                "Failed to load wallet transactions for wallet " + wallet.getId(), e);
        }
    }

    public void loadAllPendingTransactions() {
        try {
            List<Transaction> pendingTransactions = transactionRepository.findAllPending();
            mempoolService.loadPendingTransactions(pendingTransactions);
            if (!pendingTransactions.isEmpty()) {
                System.out.println("✓ " + pendingTransactions.size() + " transactions en attente chargées dans le mempool");
            }
        } catch (Exception e) {
            logger.severe("Error loading pending transactions into mempool: " + e.getMessage());
            throw new TransactionRepository.TransactionRepositoryException(
                "Failed to load pending transactions", e);
        }
    }

    private String getTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();

        if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "now";
        }
    }

    class FeeCalculatorFactory {
        public FeeCalculator getFeeCalculator(CryptoType type, FeeLevel feeLevel) {
            double multiplier = feeLevel.getMultiplier();
            if (type == CryptoType.BITCOIN) {
                return new BitcoinFeeCalculator(250, 0.0002 * multiplier);
            } else {
                return new EthereumFeeCalculator(21000, 0.00003 * multiplier);
            }
        }
    }
}
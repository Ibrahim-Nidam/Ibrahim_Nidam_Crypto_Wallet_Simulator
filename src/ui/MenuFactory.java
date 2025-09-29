package ui;

import model.Mempool;
import repository.WalletRepository;
import repository.TransactionRepository;
import service.WalletService;
import service.TransactionService;
import service.MempoolService;

public class MenuFactory {
    public static Menu createMenu() {
        WalletRepository walletRepository = new WalletRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        Mempool mempool = new Mempool();
        MempoolService mempoolService = new MempoolService(mempool);
        WalletService walletService = new WalletService(walletRepository);
        TransactionService transactionService = new TransactionService(transactionRepository, mempoolService, walletService);
        return new Menu(walletService, transactionService, mempoolService);
    }
}
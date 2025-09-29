package app;

import ui.Menu;
import ui.MenuFactory;

public class Main {

    public static void main(String[] args) {
        Menu menu = MenuFactory.createMenu();
        menu.start();
    }
}
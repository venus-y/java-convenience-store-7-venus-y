package store;

import store.controller.StoreController;

public class Application {
    public static void main(String[] args) {
        StoreController storeController = new StoreController();
        storeController.run();
    }
}

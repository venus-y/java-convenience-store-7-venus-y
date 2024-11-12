package store.controller;

import camp.nextstep.edu.missionutils.DateTimes;
import store.Product;
import store.Promotion;
import store.Receipt;
import store.Separator;
import store.StoreManager;
import store.constant.ErrorMessage;
import store.view.InputView;
import store.view.OutputView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static store.constant.ErrorMessage.EXCEEDS_AVAILABLE_STOCK;
import static store.constant.ErrorMessage.INVALID_INPUT;
import static store.constant.ErrorMessage.INVALID_ORDER_PRODUCT_FORMAT;
import static store.constant.ErrorMessage.INVALID_QUANTITY_ZERO;
import static store.constant.ErrorMessage.PRODUCT_NOT_FOUND;
import static store.constant.Regex.KOREAN_PRODUCT_ORDER_PATTERN;
import static store.constant.Regex.YES_NO_RESPONSE_FORMAT;

public class StoreController {
    private static final Path PRODUCTS_FILE_PATH = Paths.get("src/main/resources/products.md");
    private static final Path PROMOTIONS_FILE_PATH = Paths.get("src/main/resources/promotions.md");

    private static final int MINIMUM_STOCK = 1;
    private static final String EXIT_RESPONSE = "N";

    private final StoreManager storeManager = new StoreManager();
    private final OutputView outputView = new OutputView();
    private final InputView inputView = new InputView();


    public void run() {
        initializeStoreStatus(storeManager);
        processOrder();
    }

    private void showCurrentStatus() {
        Map<String, Product> generalProductInventory = storeManager.getGeneralProductInventory();
        Map<String, Product> eventProductInventory = storeManager.getEventProductInventory();
        outputView.showCurrentStatus(generalProductInventory, eventProductInventory);
    }

    private void initializeStoreStatus(StoreManager storeManager) {
        List<String> storeProducts = readFile(PRODUCTS_FILE_PATH);
        List<String> storePromotions = readFile(PROMOTIONS_FILE_PATH);
        storeManager.setUpStore(storeProducts);
        storeManager.setUpPromotions(storePromotions);
    }

    public void processOrder() {
        boolean isContinueShopping = true;
        while (isContinueShopping) {
            try {
                showCurrentStatus();
                String forContinueShopping = processOrderAndPromptForAdditionalShopping();
                isContinueShopping = checkForContinueShopping(forContinueShopping);
            } catch (IllegalArgumentException e) {
                outputView.displayErrorMessage(e.getMessage());
            }
        }
    }

    private String processOrderAndPromptForAdditionalShopping() {
        String orderItems = getOrderItems();
        String membershipChoice = getMembershipChoice();
        Receipt receipt = storeManager.processOrder(orderItems, storeManager.getEventProductInventory(), storeManager.getGeneralProductInventory(), membershipChoice);
        outputView.showReceipt(receipt, membershipChoice);
        return getForContinueShopping();
    }

    private boolean checkForContinueShopping(String forContinueShopping) {
        if (forContinueShopping.equals(EXIT_RESPONSE)) {
            return false;
        }
        return true;
    }

    private String getOrderItems() {
        while (true) {
            try {
                String orderItems = inputView.promptOrderItems();
                validateInputOrderItems(orderItems);
                return orderItems;
            } catch (IllegalArgumentException e) {
                outputView.displayErrorMessage(e.getMessage());
            }
        }
    }

    private void validateInputOrderItems(String orderItems) {
        validateInputByRegex(orderItems, KOREAN_PRODUCT_ORDER_PATTERN.getValue(), INVALID_ORDER_PRODUCT_FORMAT);
        validateProductExists(orderItems, storeManager.getGeneralProductInventory(), PRODUCT_NOT_FOUND);
        validateRequestOrderQuantity(orderItems, INVALID_QUANTITY_ZERO);
        validateExceedsQuantity(orderItems);
    }

    private void validateRequestOrderQuantity(String orderItems, ErrorMessage errorMessage) {
        final int INVALID_QUANTITY_ZERO = 0;
        String[] separatedOrderItems = Separator.separate(orderItems);
        for (String orderItem : separatedOrderItems) {
            int requestOrderQuantity = Separator.separateProductQuantity(orderItem);
            if (requestOrderQuantity <= INVALID_QUANTITY_ZERO) {
                throw new IllegalArgumentException(errorMessage.getValue());
            }
        }
    }

    private void validateProductExists(String orderItems, Map<String, Product> generalProductInventory, ErrorMessage errorMessage) {
        for (String separatedOrderItem : Separator.separate(orderItems)) {
            String productName = Separator.separateProductName(separatedOrderItem);
            if (!generalProductInventory.containsKey(productName)) {
                throw new IllegalArgumentException(errorMessage.getValue());
            }
        }
    }

    private void validateExceedsQuantity(String orderItems) {
        Map<String, Product> eventProductInventory = storeManager.getEventProductInventory();
        Map<String, Product> generalProductInventory = storeManager.getGeneralProductInventory();
        Map<String, Promotion> promotionInventory = storeManager.getPromotionInventory();
        for (String orderItem : Separator.separate(orderItems)) {
            String productName = Separator.separateProductName(orderItem);
            validateStockAvailability(orderItem, eventProductInventory, productName, promotionInventory, generalProductInventory);
        }
    }

    private void validateStockAvailability(String orderItem, Map<String, Product> eventProductInventory, String productName, Map<String, Promotion> promotionInventory, Map<String, Product> generalProductInventory) {
        if (eventProductInventory.get(productName) != null) {
            Product eventProduct = eventProductInventory.get(productName);
            Promotion promotion = promotionInventory.get(eventProduct.getPromotion());
            if (validatePromotionItem(promotion, eventProductInventory, eventProduct.getName())) {
                Product generalProduct = generalProductInventory.get(eventProduct.getName());
                int requestedOrderItemsQuantity = Separator.separateProductQuantity(orderItem);
                validateCombinedStockForEventProduct(eventProduct, generalProduct, requestedOrderItemsQuantity);
                return;
            }
        }
        Product generalProduct = generalProductInventory.get(productName);
        validateStockForGeneralProduct(orderItem, generalProduct);
    }

    private void validateCombinedStockForEventProduct(Product eventProduct, Product generalProduct, int requestedOrderItemsQuantity) {
        if (eventProduct.getQuantity() + generalProduct.getQuantity() < requestedOrderItemsQuantity) {
            throw new IllegalArgumentException(EXCEEDS_AVAILABLE_STOCK.getValue());
        }
    }

    private void validateStockForGeneralProduct(String orderItem, Product generalProduct) {
        if (generalProduct.getQuantity() < Separator.separateProductQuantity(orderItem)) {
            throw new IllegalArgumentException(EXCEEDS_AVAILABLE_STOCK.getValue());
        }
    }
    
    private boolean validatePromotionDuration(Promotion promotion) {
        LocalDateTime now = DateTimes.now();
        return now.isAfter(promotion.getStartTime()) && now.isBefore(promotion.getEndTime());
    }

    private boolean validatePromotionItem(Promotion promotion, Map<String, Product> eventProductInventory, String productName) {
        return validatePromotionDuration(promotion) && isEventStockGreaterThanZero(eventProductInventory, productName);
    }

    private boolean isEventStockGreaterThanZero(Map<String, Product> eventProductInventory, String productName) {
        Product eventProduct = eventProductInventory.get(productName);
        return MINIMUM_STOCK <= eventProduct.getQuantity();
    }

    private String getForContinueShopping() {
        while (true) {
            try {
                String forContinueShopping = inputView.promptForContinueShopping();
                validateInputByRegex(forContinueShopping, YES_NO_RESPONSE_FORMAT.getValue(), INVALID_INPUT);
                return forContinueShopping;
            } catch (IllegalArgumentException e) {
                outputView.displayErrorMessage(e.getMessage());
            }
        }
    }

    private String getMembershipChoice() {
        while (true) {
            try {
                String membershipChoice = inputView.promptForMembershipChoice();
                validateInputByRegex(membershipChoice, YES_NO_RESPONSE_FORMAT.getValue(), INVALID_INPUT);
                return membershipChoice;
            } catch (IllegalArgumentException e) {
                outputView.displayErrorMessage(e.getMessage());
            }
        }
    }

    private void validateInputByRegex(String input, String requiredPattern, ErrorMessage errorMessage) {
        Pattern pattern = Pattern.compile(requiredPattern);
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            throw new IllegalArgumentException(errorMessage.getValue());
        }
    }

    private List<String> readFile(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

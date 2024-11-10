package store.controller;

import store.Product;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static store.constant.ErrorMessage.INVALID_INPUT;
import static store.constant.ErrorMessage.INVALID_ORDER_PRODUCT_FORMAT;
import static store.constant.ErrorMessage.INVALID_QUANTITY_ZERO;
import static store.constant.ErrorMessage.PRODUCT_NOT_FOUND;
import static store.constant.Regex.KOREAN_PRODUCT_ORDER_PATTERN;
import static store.constant.Regex.YES_NO_RESPONSE_FORMAT;

public class StoreController {
    private static final Path PRODUCTS_FILE_PATH = Paths.get("src/main/resources/products.md");
    private static final Path PROMOTIONS_FILE_PATH = Paths.get("src/main/resources/promotions.md");
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
    }

    private void validateRequestOrderQuantity(String orderItems, ErrorMessage errorMessage) {
        final int INVALID_QUANTITY_ZERO = 0;
        String[] separatedOrderItems = Separator.separate(orderItems);
        for (String orderItem : separatedOrderItems) {
            int requestOrderQuantity = Integer.parseInt(Separator.separateProductQuantity(orderItem));
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

    private void validateExceedsQuantity() {
        // 이벤트 적용상품일 경우 이벤트 재고 + 일반 재고를 더 해서 수용 가능한지 체크
        // 이벤트 적용상품이 아닐 경우
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

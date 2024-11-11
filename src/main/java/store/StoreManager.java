package store;

import camp.nextstep.edu.missionutils.DateTimes;
import store.constant.ErrorMessage;
import store.view.InputView;
import store.view.OutputView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static store.constant.ErrorMessage.EXCEEDS_AVAILABLE_STOCK;
import static store.constant.ErrorMessage.INVALID_INPUT;
import static store.constant.ProductField.NAME;
import static store.constant.ProductField.PROMOTION_INFO;
import static store.constant.Regex.YES_NO_RESPONSE_FORMAT;

public class StoreManager {
    private static final int FILES_START_INDEX = 1;
    private static final String EMPTY_COLUMN = "null";
    private static final int ZERO_QUANTITY = 0;
    private static final String ACCEPTANCE_RESPONSE = "Y";
    private static final String REJECTION_RESPONSE = "N";
    private static final int BASIC_AMOUNT = 0;
    private final OutputView outputView = new OutputView();
    private final InputView inputView = new InputView();
    private final Map<String, Product> generalProductInventory = new HashMap<>();
    private final Map<String, Product> eventProductInventory = new HashMap<>();
    private final Map<String, Promotion> promotionInventory = new HashMap<>();


    public void setUpStore(List<String> storeProducts) {
        for (int index = FILES_START_INDEX; index < storeProducts.size(); index++) {
            setUpStock(storeProducts, index);
        }
        updateGeneralInventoryForMissingItems();
    }

    private void updateGeneralInventoryForMissingItems() {
        for (String key : eventProductInventory.keySet()) {
            if (!generalProductInventory.containsKey(key)) {
                Product product = eventProductInventory.get(key);
                generalProductInventory.put(key, new Product(product.getName(), product.getPrice(), ZERO_QUANTITY, EMPTY_COLUMN));
            }
        }
    }

    public void setUpPromotions(List<String> storePromotions) {
        for (int index = FILES_START_INDEX; index < storePromotions.size(); index++) {
            setUpPromotions(storePromotions, index);
        }
    }

    private void setUpPromotions(List<String> storePromotions, int index) {
        final int DAY_INCREMENT = 1;
        final int END_OF_DAY_OFFSET = 1;

        String[] promotionDetails = Separator.separate(storePromotions.get(index));
        Promotion promotion = Promotion.fromPromotionDetails(promotionDetails);
        promotionInventory.put(promotionDetails[NAME.getIndex()], promotion);
    }

    private void setUpStock(List<String> storeProducts, int index) {
        String[] productDetails = Separator.separate(storeProducts.get(index));
        Product product = Product.fromProductDetails(productDetails);
        if (productDetails[PROMOTION_INFO.getIndex()].equals(EMPTY_COLUMN)) {
            generalProductInventory.put(productDetails[NAME.getIndex()], product);
            return;
        }
        eventProductInventory.put(productDetails[NAME.getIndex()], product);
    }

    public Map<String, Product> getGeneralProductInventory() {
        return generalProductInventory;
    }

    public Map<String, Product> getEventProductInventory() {
        return eventProductInventory;
    }

    public Map<String, Promotion> getPromotionInventory() {
        return promotionInventory;
    }

    public Receipt processOrder(String orderItems, Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, String membershipChoice) {
        List<OrderItem> orderedItems = new ArrayList<>();
        List<PromotionItem> promotionItems = new ArrayList<>();
        processOrderForEachItem(orderItems, eventProductInventory, generalProductInventory, orderedItems, promotionItems);
        return new Receipt(orderedItems, promotionItems, membershipChoice);
    }

    private void processOrderForEachItem(String orderItems, Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, List<OrderItem> orderedItems, List<PromotionItem> promotionItems) {
        for (String separatedOrderItem : Separator.separate(orderItems)) {
            String requestOrderProductName = Separator.separateProductName(separatedOrderItem);
            int requestOrderProductQuantity = Separator.separateProductQuantity(separatedOrderItem);
            int initialPromotionStock = getInitialPromotionStock(eventProductInventory, requestOrderProductName);
            processOrderForPromotionStock(eventProductInventory, generalProductInventory, requestOrderProductName, requestOrderProductQuantity, orderedItems, promotionItems);
            processOrderForGeneralStock(eventProductInventory, generalProductInventory, requestOrderProductName, initialPromotionStock, requestOrderProductQuantity, orderedItems);
            processOrderOutsidePromotionDates(generalProductInventory, requestOrderProductName, requestOrderProductQuantity, orderedItems);
        }
    }

    private int getInitialPromotionStock(Map<String, Product> eventProductInventory, String requestOrderProductName) {
        if (eventProductInventory.get(requestOrderProductName) != null) {
            return eventProductInventory.get(requestOrderProductName).getQuantity();
        }
        return ZERO_QUANTITY;
    }

    private void processOrderForPromotionStock(Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, String requestOrderProductName, int requestOrderProductQuantity, List<OrderItem> orderedItems, List<PromotionItem> promotionItems) {
        if (eventProductInventory.get(requestOrderProductName) != null && validateEventStock(eventProductInventory, requestOrderProductName)) {
            Product eventProduct = eventProductInventory.get(requestOrderProductName);
            String eventProductsPromotion = eventProduct.getPromotion();
            Promotion promotion = promotionInventory.get(eventProductsPromotion);
            LocalDateTime now = DateTimes.now();
            processOrderWithActivePromotion(eventProductInventory, generalProductInventory, requestOrderProductName, requestOrderProductQuantity, orderedItems, promotionItems, now, promotion, eventProduct);
        }
    }

    private void processOrderWithActivePromotion(Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, String requestOrderProductName, int requestOrderProductQuantity, List<OrderItem> orderedItems, List<PromotionItem> promotionItems, LocalDateTime now, Promotion promotion, Product eventProduct) {
        if (now.isAfter(promotion.getStartTime()) && now.isBefore(promotion.getEndTime())) {
            int promotionStandardQuantity = promotion.getBuy() + promotion.getGet();
            int dividedRequestPromotionAvailable = requestOrderProductQuantity / promotionStandardQuantity;
            int dividedEventPromotionStockAvailable = eventProduct.getQuantity() / promotionStandardQuantity;
            applyPromotionForRequestedQuantityGreaterOrEqualToStock(eventProductInventory, generalProductInventory, dividedRequestPromotionAvailable, dividedEventPromotionStockAvailable, promotion, promotionStandardQuantity, eventProduct, requestOrderProductQuantity, orderedItems, promotionItems, requestOrderProductName);
            applyPromotionForRequestedQuantityLessThanToStock(eventProductInventory, dividedRequestPromotionAvailable, dividedEventPromotionStockAvailable, promotion, promotionStandardQuantity, eventProduct, requestOrderProductQuantity, orderedItems, promotionItems, requestOrderProductName);
        }
    }

    private void applyPromotionForRequestedQuantityGreaterOrEqualToStock(Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, int dividedRequestPromotionAvailable, int dividedEventPromotionStockAvailable, Promotion promotion, int promotionStandardQuantity, Product eventProduct, int requestOrderProductQuantity, List<OrderItem> orderedItems, List<PromotionItem> promotionItems, String requestOrderProductName) {
        if (dividedRequestPromotionAvailable >= dividedEventPromotionStockAvailable) {
            int receiveEventProductQuantity = dividedEventPromotionStockAvailable * promotion.getGet();
            int fromEventProductInventory = dividedEventPromotionStockAvailable * promotionStandardQuantity;
            eventProduct.updateQuantity(eventProduct.getQuantity() - fromEventProductInventory);
            eventProductInventory.put(eventProduct.getName(), eventProduct);
            int leftRequestOrderProductQuantity = requestOrderProductQuantity - fromEventProductInventory;
            processOrderWhenNoRemainingQuantity(eventProduct, requestOrderProductQuantity, orderedItems, promotionItems, leftRequestOrderProductQuantity, receiveEventProductQuantity);
            processOrderWhenRemainingQuantityExists(eventProductInventory, generalProductInventory, eventProduct, requestOrderProductQuantity, orderedItems, promotionItems, requestOrderProductName, leftRequestOrderProductQuantity, receiveEventProductQuantity);
        }
    }

    private void processOrderWhenNoRemainingQuantity(Product eventProduct, int requestOrderProductQuantity, List<OrderItem> orderedItems, List<PromotionItem> promotionItems, int leftRequestOrderProductQuantity, int receiveEventProductQuantity) {
        if (leftRequestOrderProductQuantity == ZERO_QUANTITY) {
            orderedItems.add(new OrderItem(eventProduct.getName(), requestOrderProductQuantity, eventProduct.getPrice(), eventProduct.getPrice() * requestOrderProductQuantity, 0, receiveEventProductQuantity));
            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
        }
    }

    private void processOrderWhenRemainingQuantityExists(Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, Product eventProduct, int requestOrderProductQuantity, List<OrderItem> orderedItems, List<PromotionItem> promotionItems, String requestOrderProductName, int leftRequestOrderProductQuantity, int receiveEventProductQuantity) {
        if (leftRequestOrderProductQuantity > ZERO_QUANTITY) {
            String forPromotionalPurchaseDecision = getNonPromotionalPurchaseDecision(leftRequestOrderProductQuantity, requestOrderProductName, YES_NO_RESPONSE_FORMAT.getValue(), INVALID_INPUT);
            if (forPromotionalPurchaseDecision.equals(ACCEPTANCE_RESPONSE)) {
                Integer withoutPromotionalPrice = BASIC_AMOUNT;
                if (eventProduct.getQuantity() > ZERO_QUANTITY) {
                    withoutPromotionalPrice = leftRequestOrderProductQuantity * eventProduct.getPrice();
                    leftRequestOrderProductQuantity = getLeftRequestOrderProductQuantity(eventProduct, leftRequestOrderProductQuantity);
                    eventProductInventory.put(eventProduct.getName(), eventProduct);
                }
                updateOrderItemsBasedOnRemainingQuantity(generalProductInventory, leftRequestOrderProductQuantity, withoutPromotionalPrice,
                        orderedItems, promotionItems, eventProduct, requestOrderProductQuantity, receiveEventProductQuantity, requestOrderProductName);
            }
            processOrderForEligiblePromotionQuantity(forPromotionalPurchaseDecision, orderedItems, requestOrderProductName, requestOrderProductQuantity, leftRequestOrderProductQuantity, eventProduct, receiveEventProductQuantity, promotionItems);
        }
    }


    private void applyPromotionForRequestedQuantityLessThanToStock(Map<String, Product> eventProductInventory, int dividedRequestPromotionAvailable, int dividedEventPromotionStockAvailable, Promotion promotion, int promotionStandardQuantity, Product eventProduct, int requestOrderProductQuantity, List<OrderItem> orderedItems, List<PromotionItem> promotionItems, String requestOrderProductName) {
        if (dividedRequestPromotionAvailable < dividedEventPromotionStockAvailable) {
            int receiveEventProductQuantity = dividedRequestPromotionAvailable * promotion.getGet();
            int fromEventProductInventory = dividedRequestPromotionAvailable * promotionStandardQuantity;
            eventProduct.updateQuantity(eventProduct.getQuantity() - fromEventProductInventory);
            eventProductInventory.put(eventProduct.getName(), eventProduct);
            int leftRequestOrderProductQuantity = requestOrderProductQuantity - fromEventProductInventory;
            verifyAdditionalPromotionAcceptance(leftRequestOrderProductQuantity, promotion, eventProduct, orderedItems, promotionItems, requestOrderProductName, receiveEventProductQuantity, fromEventProductInventory, promotionStandardQuantity, requestOrderProductQuantity);
            processRemainingOrderWithoutPromotion(eventProductInventory, leftRequestOrderProductQuantity, promotion, eventProduct, orderedItems, requestOrderProductName, requestOrderProductQuantity, receiveEventProductQuantity, promotionItems);
        }
    }

    private void verifyAdditionalPromotionAcceptance(int leftRequestOrderProductQuantity, Promotion promotion, Product eventProduct, List<OrderItem> orderedItems, List<PromotionItem> promotionItems, String requestOrderProductName, int receiveEventProductQuantity, int fromEventProductInventory, int promotionStandardQuantity, int requestOrderProductQuantity) {
        if (leftRequestOrderProductQuantity == promotion.getBuy()) {
            String forAdditionalPromotionAcceptance = getPromptForAdditionalPromotionAcceptance(eventProduct.getName(), promotion.getGet(), YES_NO_RESPONSE_FORMAT.getValue(), INVALID_INPUT);
            processAdditionalPromotionAcceptance(orderedItems, promotionItems, requestOrderProductName, forAdditionalPromotionAcceptance, receiveEventProductQuantity, fromEventProductInventory,
                    promotionStandardQuantity, eventProduct, requestOrderProductQuantity);
        }
    }

    private void processOrderForEligiblePromotionQuantity(String forPromotionalPurchaseDecision, List<OrderItem> orderedItems, String requestOrderProductName, int requestOrderProductQuantity, int leftRequestOrderProductQuantity, Product eventProduct, int receiveEventProductQuantity, List<PromotionItem> promotionItems) {
        if (forPromotionalPurchaseDecision.equals(REJECTION_RESPONSE)) {
            orderedItems.add(new OrderItem(requestOrderProductName, requestOrderProductQuantity - leftRequestOrderProductQuantity,
                    eventProduct.getPrice(), (requestOrderProductQuantity - leftRequestOrderProductQuantity) * eventProduct.getPrice(), receiveEventProductQuantity));
            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
        }
    }

    private void processRemainingOrderWithoutPromotion(Map<String, Product> eventProductInventory, int leftRequestOrderProductQuantity, Promotion promotion, Product eventProduct, List<OrderItem> orderedItems, String requestOrderProductName, int requestOrderProductQuantity, int receiveEventProductQuantity, List<PromotionItem> promotionItems) {
        if (leftRequestOrderProductQuantity < promotion.getBuy()) {
            eventProduct.updateQuantity(eventProduct.getQuantity() - leftRequestOrderProductQuantity);
            eventProductInventory.put(eventProduct.getName(), eventProduct);
            orderedItems.add(new OrderItem(requestOrderProductName, requestOrderProductQuantity, eventProduct.getPrice(),
                    requestOrderProductQuantity * eventProduct.getPrice(), leftRequestOrderProductQuantity, receiveEventProductQuantity));
            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
        }
    }

    private void processOrderForGeneralStock(Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, String requestOrderProductName, int initialPromotionStock, int requestOrderProductQuantity, List<OrderItem> orderedItems) {
        if (eventProductInventory.get(requestOrderProductName) == null || initialPromotionStock == ZERO_QUANTITY) {
            processGeneralProductOrder(generalProductInventory, requestOrderProductName, requestOrderProductQuantity, orderedItems);
        }
    }

    private void processGeneralProductOrder(Map<String, Product> generalProductInventory, String requestOrderProductName, int requestOrderProductQuantity, List<OrderItem> orderedItems) {
        Product generalProduct = generalProductInventory.get(requestOrderProductName);
        generalProduct.updateQuantity(generalProduct.getQuantity() - requestOrderProductQuantity);
        generalProductInventory.put(generalProduct.getName(), generalProduct);
        orderedItems.add(new OrderItem(generalProduct.getName(), requestOrderProductQuantity, generalProduct.getPrice(),
                generalProduct.getPrice() * requestOrderProductQuantity, generalProduct.getPrice() * requestOrderProductQuantity, ZERO_QUANTITY));
    }

    private void processOrderOutsidePromotionDates(Map<String, Product> generalProductInventory, String requestOrderProductName, int requestOrderProductQuantity, List<OrderItem> orderedItems) {
        if (eventProductInventory.get(requestOrderProductName) != null) {
            Product eventProduct = eventProductInventory.get(requestOrderProductName);
            String eventProductsPromotion = eventProduct.getPromotion();
            Promotion eventPromotion = promotionInventory.get(eventProductsPromotion);
            LocalDateTime now = DateTimes.now();
            if (!(now.isAfter(eventPromotion.getStartTime()) && now.isBefore(eventPromotion.getEndTime()))) {
                processGeneralProductOrder(generalProductInventory, requestOrderProductName, requestOrderProductQuantity, orderedItems);
            }
        }

    }

    private void processAdditionalPromotionAcceptance(List<OrderItem> orderedItems, List<PromotionItem> promotionItems, String requestOrderProductName, String forAdditionalPromotionAcceptance,
                                                      int receiveEventProductQuantity, int fromEventProductInventory, int promotionStandardQuantity,
                                                      Product eventProduct, int requestOrderProductQuantity) {
        if (forAdditionalPromotionAcceptance.equals(ACCEPTANCE_RESPONSE)) {
            receiveEventProductQuantity++;
            fromEventProductInventory += promotionStandardQuantity;
            eventProduct.updateQuantity(eventProduct.getQuantity() - promotionStandardQuantity);
            eventProductInventory.put(eventProduct.getName(), eventProduct);
            requestOrderProductQuantity++;
            orderedItems.add(new OrderItem(requestOrderProductName, requestOrderProductQuantity, eventProduct.getPrice(),
                    requestOrderProductQuantity * eventProduct.getPrice(), BASIC_AMOUNT, receiveEventProductQuantity));
            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
        }
    }

    private String getNonPromotionalPurchaseDecision(int leftRequestOrderProductQuantity, String requestOrderProductName, String requiredPattern, ErrorMessage errorMessage) {
        while (true) {
            try {
                String forNonPromotionalPurchase = inputView.promptForNonPromotionalPurchase(leftRequestOrderProductQuantity, requestOrderProductName);
                validateInputByRegex(forNonPromotionalPurchase, requiredPattern, errorMessage);
                return forNonPromotionalPurchase;
            } catch (IllegalArgumentException e) {
                outputView.displayErrorMessage(e.getMessage());
            }
        }
    }

    private String getPromptForAdditionalPromotionAcceptance(String eventProductName, int promotionalQuantity, String requiredPattern, ErrorMessage errorMessage) {
        while (true) {
            try {
                String forAdditionalQuantity = inputView.promptForAdditionalQuantity(eventProductName, promotionalQuantity);
                validateInputByRegex(forAdditionalQuantity, requiredPattern, errorMessage);
                return forAdditionalQuantity;
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

    private void validateSufficientStock(Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, String requestOrderProductName, int requestOrderProductQuantity) {
        if (eventProductInventory.get(requestOrderProductName).getQuantity() + generalProductInventory.get(requestOrderProductName).getQuantity() < requestOrderProductQuantity) {
            throw new IllegalArgumentException(EXCEEDS_AVAILABLE_STOCK.getValue());
        }
    }

    private int getLeftRequestOrderProductQuantity(Product eventProduct, int leftRequestOrderProductQuantity) {
        if (eventProduct.getQuantity() >= leftRequestOrderProductQuantity) {
            eventProduct.updateQuantity(eventProduct.getQuantity() - leftRequestOrderProductQuantity);
            leftRequestOrderProductQuantity = ZERO_QUANTITY;
            return leftRequestOrderProductQuantity;
        }
        if (eventProduct.getQuantity() < leftRequestOrderProductQuantity) {
            leftRequestOrderProductQuantity -= eventProduct.getQuantity();
            eventProduct.updateQuantity(ZERO_QUANTITY);
        }
        return leftRequestOrderProductQuantity;
    }

    private void updateOrderItemsBasedOnRemainingQuantity(Map<String, Product> generalProductInventory, int leftRequestOrderProductQuantity, int withoutPromotionalPrice,
                                                          List<OrderItem> orderedItems, List<PromotionItem> promotionItems, Product eventProduct, int requestOrderProductQuantity, int receiveEventProductQuantity, String requestOrderProductName) {
        if (leftRequestOrderProductQuantity == ZERO_QUANTITY) {
            orderedItems.add(new OrderItem(eventProduct.getName(), requestOrderProductQuantity, eventProduct.getPrice(), eventProduct.getPrice() * requestOrderProductQuantity, withoutPromotionalPrice, receiveEventProductQuantity));
            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
            return;
        }
        if (leftRequestOrderProductQuantity > ZERO_QUANTITY) {
            Product generalProduct = generalProductInventory.get(requestOrderProductName);
            generalProduct.updateQuantity(generalProduct.getQuantity() - leftRequestOrderProductQuantity);
            generalProductInventory.put(requestOrderProductName, generalProduct);
            orderedItems.add(new OrderItem((eventProduct.getName()), requestOrderProductQuantity, eventProduct.getPrice(), eventProduct.getPrice() * requestOrderProductQuantity, withoutPromotionalPrice, receiveEventProductQuantity));
            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
        }
    }

    private boolean validateEventStock(Map<String, Product> eventProductInventory, String productName) {
        return eventProductInventory.get(productName).getQuantity() > ZERO_QUANTITY;
    }
}

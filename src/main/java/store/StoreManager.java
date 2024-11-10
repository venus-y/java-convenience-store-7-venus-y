package store;

import camp.nextstep.edu.missionutils.DateTimes;
import store.constant.ErrorMessage;
import store.view.InputView;
import store.view.OutputView;

import java.time.LocalDate;
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
import static store.constant.ProductField.PRICE;
import static store.constant.ProductField.PROMOTION_INFO;
import static store.constant.ProductField.QUANTITY;
import static store.constant.PromotionField.BUY;
import static store.constant.PromotionField.END_DATE;
import static store.constant.PromotionField.GET;
import static store.constant.PromotionField.START_DATE;
import static store.constant.Regex.YES_NO_RESPONSE_FORMAT;

public class StoreManager {
    private static final int START_INDEX = 1;
    private static final String EMPTY_COLUMN = "null";
    private static final int INITIAL_VALUE = 0;

    private final OutputView outputView = new OutputView();
    private final InputView inputView = new InputView();
    private final Map<String, Product> generalProductInventory = new HashMap<>();
    private final Map<String, Product> eventProductInventory = new HashMap<>();
    private final Map<String, Promotion> promotionInventory = new HashMap<>();


    public void setUpStore(List<String> storeProducts) {
        for (int index = START_INDEX; index < storeProducts.size(); index++) {
            setUpStock(storeProducts, index);
        }
        for (String key : eventProductInventory.keySet()) {
            if (!generalProductInventory.containsKey(key)) {
                Product product = eventProductInventory.get(key);
                generalProductInventory.put(key, new Product(product.getName(), product.getPrice(), 0, "null"));
            }
        }

    }

    public void setUpPromotions(List<String> storePromotions) {
        for (int index = START_INDEX; index < storePromotions.size(); index++) {
            setUpPromotions(storePromotions, index);
        }
    }

    private void setUpPromotions(List<String> storePromotions, int index) {
        String[] promotionDetails = Separator.separate(storePromotions.get(index));
        Promotion promotion = new Promotion(promotionDetails[NAME.getIndex()], Integer.parseInt(promotionDetails[BUY.getIndex()]),
                Integer.parseInt(promotionDetails[GET.getIndex()]), LocalDate.parse(promotionDetails[START_DATE.getIndex()]).atStartOfDay(),
                LocalDate.parse(promotionDetails[END_DATE.getIndex()]).plusDays(1).atStartOfDay().minusSeconds(1));
        promotionInventory.put(promotionDetails[NAME.getIndex()], promotion);
    }

    private void setUpStock(List<String> storeProducts, int index) {
        String[] productDetails = Separator.separate(storeProducts.get(index));
        Product product = new Product(productDetails[NAME.getIndex()], Integer.parseInt(productDetails[PRICE.getIndex()]),
                Integer.parseInt(productDetails[QUANTITY.getIndex()]), productDetails[PROMOTION_INFO.getIndex()]);
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
        // 주문상품 담는 곳
        List<OrderItem> orderedItems = new ArrayList<>();
        // 프로모션 증정품 담는 곳
        List<PromotionItem> promotionItems = new ArrayList<>();
        // 상품 목록이 담겨있는 배열 ex) "사이다-2", "감자칩-1"
        String[] separatedOrderItems = Separator.separate(orderItems);
        for (String separatedOrderItem : separatedOrderItems) {
            // 주문상품명을 추출
            String requestOrderProductName = Separator.separateProductName(separatedOrderItem);
            // 주문상품 수량을 추출
            int requestOrderProductQuantity = Integer.parseInt(Separator.separateProductQuantity(separatedOrderItem));
            // 이벤트 상품의 초기 재고를 구해서 0개인지 확인 후 일반 재고로 보낼 수 있도록 한다.
            int initialPromotionStock = 0;
            if (eventProductInventory.get(requestOrderProductName) != null) {
                Integer quantity = eventProductInventory.get(requestOrderProductName).getQuantity();
                initialPromotionStock = quantity;
            }
            // 해당 상품이 프로모션 상품인지 확인한다. 1. 프로모션 상품일 경우 아래 if문 분기를 타고, 2. 아닐 경우 일반재고처리 분기로 간다.
            // 프로모션 재고가 0개 or 프로모션 상품이 아닐 경우 일반재고에서 처리한다.
            if (eventProductInventory.get(requestOrderProductName) != null && validateEventStock(eventProductInventory, requestOrderProductName)) {
                // 이벤트재고와 일반재고가 구매요청수량을 수용 가능한지 확인한다.
                validateSufficientStock(eventProductInventory, generalProductInventory, requestOrderProductName, requestOrderProductQuantity);
                // 프로모션이 적용되는 날짜인지 확인해본다.
                Product eventProduct = eventProductInventory.get(requestOrderProductName);
                // 해당 상품의 프로모션 정보 추출
                String eventProductsPromotion = eventProduct.getPromotion();
                // 프로모션 목록을 조회해 해당 프로모션이 유효한 프로모션인지 확인한다.
                Promotion promotion = promotionInventory.get(eventProductsPromotion);
                // 오늘 날짜를 조회
                LocalDateTime now = DateTimes.now();
                // 오늘 날짜가 프로모션의 시작일과 종료일 사이에 들어가는지 확인한다.
                // 유효한 프로모션일 경우 아래 if문 분기를 타고 그렇지 않을 경우 일반 재고처리 분기로 간다.
                if (now.isAfter(promotion.getStartTime()) && now.isBefore(promotion.getEndTime())) {
                    // 프로모션이 적용되는 기준인 buy와 get을 더한 수치를 구한다.
                    int promotionStandardQuantity = promotion.getBuy() + promotion.getGet();
                    // 구매요청수량을 프로모션이 적용되는 기준인 buy+get의 수치로 나눈다.
                    // ex) 구매요청 수량이 10이고 2+1 프로모션 적용 중일 경우 10/(2+1)...
                    int dividedRequestPromotionAvailable = requestOrderProductQuantity / promotionStandardQuantity;
                    // 이벤트상품의 재고 또한 프로모션이 적용되는 기준인 buy+get의 수치로 나눈다.
                    int dividedEventPromotionStockAvailable = eventProduct.getQuantity() / promotionStandardQuantity;
                    // dividedRequestPromotionAvailable > dividedEventPromotionStockAvailable 일 경우 일단 프로모션재고에서
                    // 가능한 만큼 프로모션을 적용해본다.
                    if (dividedRequestPromotionAvailable >= dividedEventPromotionStockAvailable) {
                        // 이 경우 dividedEventPromotionStockAvailable을 기준으로 프로모션을 적용한다.
                        // dividedEventPromotionStockAvailable * promotion.get을 통해 프로모션으로 증정받는 상품의 개수를 구한다.
                        int receiveEventProductQuantity = dividedEventPromotionStockAvailable * promotion.getGet();
                        // 프로모션을 증정하는 과정에서 프로모션 재고에서 재고가 나가는 개수를 구한다.
                        // ex) 1. 10의 구매수량/2+1 프로모션 , 2. 7의 잔여재고/2+1 프로모션을 비교해보면 몫이 3, 2가 나오게 된다.
                        // 2.에서 몫인 2 * 프로모션 적용기준인 2+1을 더하면 6의 재고가 빠져나가는 것을 알 수 있다.
                        int fromEventProductInventory = dividedEventPromotionStockAvailable * promotionStandardQuantity;
                        // 이벤트재고를 업데이트한다.
                        eventProduct.updateQuantity(eventProduct.getQuantity() - fromEventProductInventory);
                        eventProductInventory.put(eventProduct.getName(), eventProduct);
                        int leftRequestOrderProductQuantity = requestOrderProductQuantity - fromEventProductInventory;
                        // 총 구매요청수량에서 10에서 6을 빼 추가적으로 계산해야 할 구매수량인 4를 구한다.
                        // 하지만 만약 구매요청수량이 9, 재고에 있던 수량도 9라면 다음 연산이 필요하지 않게 된다.
                        // 따라서 다음 leftRequestOrderProductQuantity가 0이 되는지 검증한다.
                        if (leftRequestOrderProductQuantity == 0) {
                            // 구매요청수량을 전부 소화했을 경우 주문상품 목록을 업데이트한다.
                            orderedItems.add(new OrderItem(eventProduct.getName(), requestOrderProductQuantity, eventProduct.getPrice(), eventProduct.getPrice() * requestOrderProductQuantity, 0, receiveEventProductQuantity));
                            // 증정받은 이벤트 상품을 추가한다.
                            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
                        }
                        // 아직 구매할 수량이 남았다면 다음 흐름을 이어서 진행한다.
                        if (leftRequestOrderProductQuantity > 0) {
                            // 구매요청수량 - 프로모션 적용 결과의 개수를 구하고 잔여수량이 있을 경우 일부 수량을 프로모션 없이 결제해야 한다는 메세지를 출력한다.
                            // 메세지 출력 후 정가로 구매할 것이냐는 의사를 묻고 Y일 경우 정가로 나머지 상품들을 결제하고, N일 경우
                            // 남은 주문요청수량을 결제하지 않게 된다.
                            // 입력값 검증 해야 한다.
                            String forPromotionalPurchaseDecision = getNonPromotionalPurchaseDecision(leftRequestOrderProductQuantity, requestOrderProductName, YES_NO_RESPONSE_FORMAT.getValue(), INVALID_INPUT);
                            if (forPromotionalPurchaseDecision.equals("Y")) {
                                // 프로모션 할인이 적용되지 않는 금액
                                Integer withoutPromotionalPrice = 0;
                                if (eventProduct.getQuantity() > 0) {
                                    // 프로모션이 적용되지 않는 상태에서 프로모션 재고에서 꺼내온 상품은 프로모션 미적용금액으로 판정한다.
                                    withoutPromotionalPrice = leftRequestOrderProductQuantity * eventProduct.getPrice();
                                    // 이벤트 재고에 남은 상품이 있을 경우 남은 구매요청 수량에서 차감한다.
                                    // 이벤트재고가 잔여수량보다 많거나 같을시 , 이벤트재고가 잔여수량보다 적을 시
                                    leftRequestOrderProductQuantity = getLeftRequestOrderProductQuantity(eventProduct, leftRequestOrderProductQuantity);
                                    eventProductInventory.put(eventProduct.getName(), eventProduct);
                                }
                                // 이벤트 재고에서 잔여수량을 차감했음에도 추가적으로 처리할 구매요청수량이 남아있는지 확인한다.
                                updateOrderItemsBasedOnRemainingQuantity(generalProductInventory, leftRequestOrderProductQuantity, withoutPromotionalPrice,
                                        orderedItems, promotionItems, eventProduct, requestOrderProductQuantity, receiveEventProductQuantity, requestOrderProductName);
                            }
                            // 프로모션이 적용되는 데까지만 결제할 경우에 해당된다.
                            if (forPromotionalPurchaseDecision.equals("N")) {
                                orderedItems.add(new OrderItem(requestOrderProductName, requestOrderProductQuantity - leftRequestOrderProductQuantity,
                                        eventProduct.getPrice(), (requestOrderProductQuantity - leftRequestOrderProductQuantity) * eventProduct.getPrice(), receiveEventProductQuantity));
                                // 증정받은 이벤트 상품을 추가한다.
                                promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
                            }
                        }
                    }
                    if (dividedRequestPromotionAvailable < dividedEventPromotionStockAvailable) {
                        // 구매요청 수량/buy+get < 이벤트재고수량/buy+get일 경우
                        // 이 경우 dividedRequestPromotionAvailable 기준으로 프로모션을 적용한다.
                        // dividedRequestPromotionAvailable * promotion.get을 통해 프로모션으로 증정받는 상품의 개수를 구한다.
                        int receiveEventProductQuantity = dividedRequestPromotionAvailable * promotion.getGet();
                        // 프로모션을 증정하는 과정에서 프로모션 재고에서 재고가 나가는 개수를 구한다.
                        // ex) 1. 6의 구매수량/2+1 프로모션 , 2. 10의 잔여재고/2+1 프로모션을 비교해보면 몫이 2, 3이 나오게 된다.
                        // 1.에서 몫인 2 * 프로모션 적용기준인 2+1을 더하면 6의 재고가 빠져나가는 것을 알 수 있다.
                        // 프로모션 적용 시 프로모션 재고에서 빠져나가게 될 상품의 수를 계산한다.
                        int fromEventProductInventory = dividedRequestPromotionAvailable * promotionStandardQuantity;
                        // 이벤트 재고를 업데이트한다.
                        eventProduct.updateQuantity(eventProduct.getQuantity() - fromEventProductInventory);
                        eventProductInventory.put(eventProduct.getName(), eventProduct);
                        // 프로모션을 적용하고 남은 잔여수량이 있는지 구한다.
                        int leftRequestOrderProductQuantity = requestOrderProductQuantity - fromEventProductInventory;
                        // 잔여수량이 프로모션을 한번 더 적용할 수 있는 경우라면
                        // "현재 {상품명}은(는) 1개를 무료로 더 받을 수 있습니다. 추가하시겠습니까? (Y/N)" 메시지를 출력 후 구매자가 상품을 추가할 것인지 결정하게 한다.
                        if (leftRequestOrderProductQuantity == promotion.getBuy()) {
                            // 입력값 검증 해야 한다.
                            String forAdditionalPromotionAcceptance = getPromptForAdditionalPromotionAcceptance(eventProduct.getName(), promotion.getGet(), YES_NO_RESPONSE_FORMAT.getValue(), INVALID_INPUT);
                            if (forAdditionalPromotionAcceptance.equals("Y")) {
                                receiveEventProductQuantity++;
                                fromEventProductInventory += promotionStandardQuantity;
                                eventProduct.updateQuantity(eventProduct.getQuantity() - promotionStandardQuantity);
                                eventProductInventory.put(eventProduct.getName(), eventProduct);
                                // 추가로 받은 개수를 구매요청수량에 추가한다.
                                requestOrderProductQuantity++;
                                orderedItems.add(new OrderItem(requestOrderProductName, requestOrderProductQuantity, eventProduct.getPrice(),
                                        requestOrderProductQuantity * eventProduct.getPrice(), 0, receiveEventProductQuantity));
                                // 증정받은 이벤트 상품을 추가한다.
                                promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
                            }
                        }
                        // 잔여수량이 프로모션을 한번 더 적용할 수 없는 경우라면 프로모션 재고에서 잔여수량을 차감하는 것으로 마친다.
                        if (leftRequestOrderProductQuantity < promotion.getBuy()) {
                            eventProduct.updateQuantity(eventProduct.getQuantity() - leftRequestOrderProductQuantity);
                            eventProductInventory.put(eventProduct.getName(), eventProduct);
                            orderedItems.add(new OrderItem(requestOrderProductName, requestOrderProductQuantity, eventProduct.getPrice(),
                                    requestOrderProductQuantity * eventProduct.getPrice(), leftRequestOrderProductQuantity, receiveEventProductQuantity));
                            // 증정받은 이벤트 상품을 추가한다.
                            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
                        }
                    }
                }
            }
            // 구매하려는 상품이 프로모션이 적용되지 않거나, 해당 상품의 프로모션 재고가 0일 경우 일반 재고에서 처리하게 된다.
            if (eventProductInventory.get(requestOrderProductName) == null || initialPromotionStock == 0) {
                // 일반재고에 해당 상품을 수용할 수 있을만큼의 재고가 있는지 확인한다.
                validateSufficientStock(generalProductInventory, requestOrderProductName, requestOrderProductQuantity);
                Product generalProduct = generalProductInventory.get(requestOrderProductName);
                generalProduct.updateQuantity(generalProduct.getQuantity() - requestOrderProductQuantity);
                generalProductInventory.put(generalProduct.getName(), generalProduct);
                orderedItems.add(new OrderItem(generalProduct.getName(), requestOrderProductQuantity, generalProduct.getPrice(),
                        generalProduct.getPrice() * requestOrderProductQuantity, generalProduct.getPrice() * requestOrderProductQuantity, 0));
            }
            // 프로모션 적용 시간을 만족하지 못한 경우를 체크한다.
            // 프로모션은 적용 가능하지만 날짜를 만족치 못하는 경우
            if (eventProductInventory.get(requestOrderProductName) != null) {
                Product eventProduct = eventProductInventory.get(requestOrderProductName);
                String eventProductsPromotion = eventProduct.getPromotion();
                Promotion eventPromotion = promotionInventory.get(eventProductsPromotion);

                LocalDateTime now = DateTimes.now();
                if (!(now.isAfter(eventPromotion.getStartTime()) && now.isBefore(eventPromotion.getEndTime()))) {
                    validateSufficientStock(generalProductInventory, requestOrderProductName, requestOrderProductQuantity);
                    Product generalProduct = generalProductInventory.get(requestOrderProductName);
                    generalProduct.updateQuantity(generalProduct.getQuantity() - requestOrderProductQuantity);
                    generalProductInventory.put(generalProduct.getName(), generalProduct);
                    orderedItems.add(new OrderItem(generalProduct.getName(), requestOrderProductQuantity, generalProduct.getPrice(),
                            generalProduct.getPrice() * requestOrderProductQuantity, generalProduct.getPrice() * requestOrderProductQuantity, 0));
                }
            }
        }
        // 총 주문금액
        int totalOrderedPrice = orderedItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
        // 총 주문수량
        int totalOrderedQuantity = orderedItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
        // 프로모션 미적용 금액
        int totalPayWithoutPromotion = orderedItems.stream()
                .mapToInt(OrderItem::getPayWithoutPromotionPrice)
                .sum();
        double applyMemberShipServiceAmount = 0;
        if (membershipChoice.equals("Y")) {
            applyMemberShipServiceAmount = totalPayWithoutPromotion * 0.3;
        }
        if (applyMemberShipServiceAmount >= 8000) {
            applyMemberShipServiceAmount = 8000;
        }
        int promotionalDiscountPrice = promotionItems.stream()
                .mapToInt(PromotionItem::promotionalDiscountPrice)
                .sum();
        double finalPrice = totalOrderedPrice - promotionalDiscountPrice - applyMemberShipServiceAmount;
        return new Receipt(orderedItems, promotionItems, totalOrderedPrice, totalOrderedQuantity, membershipChoice, (int) applyMemberShipServiceAmount, promotionalDiscountPrice, (int) finalPrice);
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

    // 이벤트 재고 조회
    private void validateSufficientStock(Map<String, Product> eventProductInventory, Map<String, Product> generalProductInventory, String requestOrderProductName, int requestOrderProductQuantity) {
        if (eventProductInventory.get(requestOrderProductName).getQuantity() + generalProductInventory.get(requestOrderProductName).getQuantity() < requestOrderProductQuantity) {
            throw new IllegalArgumentException(EXCEEDS_AVAILABLE_STOCK.getValue());
        }
    }

    // 프로모션 적용 후 이벤트 재고와 잔여수량을 비교하고 잔여수량을 처리하는 메서드
    private int getLeftRequestOrderProductQuantity(Product eventProduct, int leftRequestOrderProductQuantity) {
        if (eventProduct.getQuantity() >= leftRequestOrderProductQuantity) {
            eventProduct.updateQuantity(eventProduct.getQuantity() - leftRequestOrderProductQuantity);
            leftRequestOrderProductQuantity = 0;
            return leftRequestOrderProductQuantity;
        }
        if (eventProduct.getQuantity() < leftRequestOrderProductQuantity) {
            leftRequestOrderProductQuantity -= eventProduct.getQuantity();
            eventProduct.updateQuantity(0);
        }
        return leftRequestOrderProductQuantity;
    }

    // 일반 재고 조회
    private void validateSufficientStock(Map<String, Product> generalProductInventory, String requestOrderProductName, int requestOrderProductQuantity) {
        if (generalProductInventory.get(requestOrderProductName).getQuantity() < requestOrderProductQuantity) {
            throw new IllegalArgumentException(EXCEEDS_AVAILABLE_STOCK.getValue());
        }
    }

    // 추가적으로 처리해야 할 주문수량이 남아있는 경우를 대비해 두 개의 if를 사용해서 처리한다.
    private void updateOrderItemsBasedOnRemainingQuantity(Map<String, Product> generalProductInventory, int leftRequestOrderProductQuantity, int withoutPromotionalPrice,
                                                          List<OrderItem> orderedItems, List<PromotionItem> promotionItems, Product eventProduct, int requestOrderProductQuantity, int receiveEventProductQuantity, String requestOrderProductName) {
        if (leftRequestOrderProductQuantity == 0) {
            orderedItems.add(new OrderItem(eventProduct.getName(), requestOrderProductQuantity, eventProduct.getPrice(), eventProduct.getPrice() * requestOrderProductQuantity, withoutPromotionalPrice, receiveEventProductQuantity));
            // 증정받은 이벤트 상품을 추가한다.
            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
            return;
        }
        // 아직도 남은 재고가 있다면 일반 재고에서 차감하면 된다.
        if (leftRequestOrderProductQuantity > 0) {
            Product generalProduct = generalProductInventory.get(requestOrderProductName);
            // 일반재고에서 차감된 상품의 수량 * 가격을 구한다.
            generalProduct.updateQuantity(generalProduct.getQuantity() - leftRequestOrderProductQuantity);
            generalProductInventory.put(generalProduct.getName(), generalProduct);
            orderedItems.add(new OrderItem(eventProduct.getName(), requestOrderProductQuantity, eventProduct.getPrice(), eventProduct.getPrice() * requestOrderProductQuantity, withoutPromotionalPrice, receiveEventProductQuantity));
            promotionItems.add(new PromotionItem(eventProduct.getName(), receiveEventProductQuantity, eventProduct.getPrice()));
        }
    }

    private boolean validateEventStock(Map<String, Product> eventProductInventory, String productName) {
        return eventProductInventory.get(productName).getQuantity() > 0;
    }
}

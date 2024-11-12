package store.constant;

public enum ErrorMessage {
    ERROR_PREFIX("[ERROR]"),
    INVALID_ORDER_PRODUCT_FORMAT(ERROR_PREFIX.getValue() + " 올바르지 않은 형식으로 입력했습니다. 다시 입력해 주세요."),
    INVALID_INPUT(ERROR_PREFIX.getValue() + " 잘못된 입력입니다. 다시 입력해주세요"),
    PRODUCT_NOT_FOUND(ERROR_PREFIX.getValue() + " 존재하지 않는 상품입니다. 다시 입력해 주세요."),
    EXCEEDS_AVAILABLE_STOCK(ERROR_PREFIX.getValue() + " 재고 수량을 초과하여 구매할 수 없습니다. 다시 입력해 주세요."),
    INVALID_QUANTITY_ZERO(ERROR_PREFIX.getValue() + " 구매수량은 0개 이하일 수 없습니다");


    private final String value;

    private ErrorMessage(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

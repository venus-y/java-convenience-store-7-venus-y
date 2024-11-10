package store.constant;

public enum Regex {
    KOREAN_PRODUCT_ORDER_PATTERN("^\\[[가-힣]+-\\d+\\](,\\[[가-힣]+-\\d+\\])*$"), YES_NO_RESPONSE_FORMAT("^[YN]$");


    private final String value;

    private Regex(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

package store.constant;

public enum ProductField {
    NAME(0), PRICE(1), QUANTITY(2), PROMOTION_INFO(3);

    private final int index;

    ProductField(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}

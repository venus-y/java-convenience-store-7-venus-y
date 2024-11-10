package store.constant;

public enum PromotionField {
    NAME(0), BUY(1), GET(2), START_DATE(3), END_DATE(4);

    private final int index;

    private PromotionField(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}

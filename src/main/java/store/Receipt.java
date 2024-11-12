package store;

import java.util.List;

public class Receipt {
    private final List<OrderItem> orderedItems;
    private final List<PromotionItem> promotionItems;
    private final int totalPrice;
    private final int totalQuantity;
    private final boolean isMembershipActive;
    private final int memberShipDiscountPrice;
    private final int promotionalDiscountPrice;
    private final int finalPrice;


    public Receipt(List<OrderItem> orderedItems, List<PromotionItem> promotionItems, String memberShipChoice) {

        this.orderedItems = orderedItems;
        this.promotionItems = promotionItems;
        this.isMembershipActive = updateMembershipChoice(memberShipChoice);
        this.totalPrice = orderedItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
        this.totalQuantity = orderedItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
        int totalPayWithoutPromotion = orderedItems.stream()
                .mapToInt(OrderItem::getPayWithoutPromotionPrice)
                .sum();
        this.memberShipDiscountPrice = getMembershipDiscountPrice(memberShipChoice, totalPayWithoutPromotion);

        this.promotionalDiscountPrice = promotionItems.stream()
                .mapToInt(PromotionItem::promotionalDiscountPrice)
                .sum();
        this.finalPrice = totalPrice - promotionalDiscountPrice - memberShipDiscountPrice;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getMembershipDiscountPrice(String membershipChoice, int totalPayWithoutPromotion) {
        final String ACCEPTANCE_RESPONSE = "Y";
        final double MEMBERSHIP_DISCOUNT_RATE = 0.3;
        final int NO_MEMBERSHIP_DISCOUNT = 0;

        if (membershipChoice.equals(ACCEPTANCE_RESPONSE)) {
            double membershipDiscountPrice = totalPayWithoutPromotion * MEMBERSHIP_DISCOUNT_RATE;
            return getDiscountWithinLimit(membershipDiscountPrice);
        }

        return NO_MEMBERSHIP_DISCOUNT;
    }

    public int getDiscountWithinLimit(double membershipDiscountPrice) {
        final int MAXIMUM_MEMBERSHIP_DISCOUNT = 8000;
        if (membershipDiscountPrice >= MAXIMUM_MEMBERSHIP_DISCOUNT) {
            return MAXIMUM_MEMBERSHIP_DISCOUNT;
        }
        return (int) membershipDiscountPrice;
    }

    public List<OrderItem> getOrderedItems() {
        return orderedItems;
    }

    public List<PromotionItem> getPromotionItems() {
        return promotionItems;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public boolean isMembershipActive() {
        return isMembershipActive;
    }

    public int getMemberShipDiscountPrice() {
        return memberShipDiscountPrice;
    }

    public int getPromotionalDiscountPrice() {
        return promotionalDiscountPrice;
    }

    public int getFinalPrice() {
        return finalPrice;
    }

    private boolean updateMembershipChoice(String membershipChoice) {
        if (membershipChoice.equals("Y")) {
            return true;
        }
        return false;
    }
}

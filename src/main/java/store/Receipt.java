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


    public Receipt(List<OrderItem> orderedItems, List<PromotionItem> promotionItems, int totalPrice, int totalQuantity, String memberShipChoice, int memberShipDiscountPrice, int promotionalDiscountPrice, int finalPrice) {
        this.orderedItems = orderedItems;
        this.promotionItems = promotionItems;
        this.totalPrice = totalPrice;
        this.totalQuantity = totalQuantity;
        this.isMembershipActive = updateMembershipChoice(memberShipChoice);
        this.memberShipDiscountPrice = memberShipDiscountPrice;
        this.promotionalDiscountPrice = promotionalDiscountPrice;
        this.finalPrice = finalPrice;
    }

    public int getTotalQuantity() {
        return totalQuantity;
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

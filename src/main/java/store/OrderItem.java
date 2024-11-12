package store;

public class OrderItem {
    private String name;
    private int quantity;
    private int price;
    private int totalPrice;
    private int payWithoutPromotionPrice;
    private int promotionQuantity;

    public OrderItem(String name, int quantity, int price, int totalPrice, int promotionQuantity) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = totalPrice;
        this.promotionQuantity = promotionQuantity;
    }

    public OrderItem(String name, int quantity, int price, int totalPrice, int payFromGeneralInventoryPrice, int promotionQuantity) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = totalPrice;
        this.payWithoutPromotionPrice = payFromGeneralInventoryPrice;
        this.promotionQuantity = promotionQuantity;
    }

    public OrderItem(String name, int quantity, int price, int totalPrice) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = totalPrice;
    }

    public int getPromotionalDiscountPrice() {
        return price * promotionQuantity;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public int getPayWithoutPromotionPrice() {
        return payWithoutPromotionPrice;
    }

    public int getPromotionQuantity() {
        return promotionQuantity;
    }
}

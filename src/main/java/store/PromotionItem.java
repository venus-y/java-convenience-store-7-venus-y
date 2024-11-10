package store;

public class PromotionItem {
    private String name;
    private int quantity;
    private int price;


    public PromotionItem(String name, int quantity, int price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
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

    public int promotionalDiscountPrice() {
        return this.quantity * this.price;
    }
}

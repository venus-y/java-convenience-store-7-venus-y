package store;

import static store.constant.ProductField.NAME;
import static store.constant.ProductField.PRICE;
import static store.constant.ProductField.PROMOTION_INFO;
import static store.constant.ProductField.QUANTITY;

public class Product {
    private String name;
    private int price;

    private Integer quantity;

    private String promotion;

    public Product(String name, int price, int quantity, String promotion) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.promotion = promotion;
    }

    public static Product fromProductDetails(String[] productDetails) {
        String name = productDetails[NAME.getIndex()];
        int price = Integer.parseInt(productDetails[PRICE.getIndex()]);
        int quantity = Integer.parseInt(productDetails[QUANTITY.getIndex()]);
        String promotion = productDetails[PROMOTION_INFO.getIndex()];

        return new Product(name, price, quantity, promotion);
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getPromotion() {
        return promotion;
    }
}

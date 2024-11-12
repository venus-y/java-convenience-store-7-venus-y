package store.constant;

import java.util.HashMap;
import java.util.Map;

public enum ProductType {
    COLA("콜라", 1000, "탄산2+1"),
    SPRITE("사이다", 1000, "탄산2+1"),
    ORANGE_JUICE("오렌지주스", 1800, "MD추천상품"),
    SPARKLING_WATER("탄산수", 1200, "탄산2+1"),
    WATER("물", 500, "none"),
    VITAMIN_WATER("비타민워터", 1500, "none"),
    POTATO_CHIPS("감자칩", 1500, "반짝할인"),
    CHOCOLATE_BAR("초코바", 1200, "MD추천상품"),
    ENERGY_BAR("에너지바", 2000, "none"),
    RICE_MEAL("정식도시락", 6400, "none"),
    CUP_NOODLES("컵라면", 1700, "MD추천상품");

    private static final Map<String, ProductType> NAME_MAP = new HashMap<>();

    static {
        for (ProductType type : values()) {
            NAME_MAP.put(type.getName(), type);
        }
    }

    private final String name;
    private final int price;
    private final String promotion;

    private ProductType(String name, int price, String promotion) {
        this.name = name;
        this.price = price;
        this.promotion = promotion;
    }

    public static ProductType findByName(String name) {
        return NAME_MAP.get(name);
    }

    public String getName() {
        return name;
    }

    public String getPromotion() {
        return promotion;
    }

    public int getPrice() {
        return price;
    }
}

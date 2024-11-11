package store.view;

import store.OrderItem;
import store.Product;
import store.PromotionItem;
import store.Receipt;
import store.constant.ProductType;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class OutputView {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###");
    private static final int EMPTY_STOCK = 0;

    private void displayOrderedProductsDetails(Receipt receipt) {
        List<OrderItem> orderedItems = receipt.getOrderedItems();
        for (OrderItem orderedItem : orderedItems) {
            String orderedPrice = DECIMAL_FORMAT.format(orderedItem.getTotalPrice());
            System.out.printf("%-15s %3d %10d%n", orderedItem.getName(), orderedItem.getQuantity(), orderedItem.getTotalPrice());
        }
    }

    private void displayTotalResult(Receipt receipt, String totalPrice) {
        System.out.println("===============================");
        System.out.println("총구매액" + "\t\t\t\t" + receipt.getTotalQuantity() + "\t\t" + totalPrice);
        System.out.println("행사할인\t\t\t\t\t\t -" + DECIMAL_FORMAT.format(receipt.getPromotionalDiscountPrice()));
        System.out.println("멤버십할인\t\t\t\t\t -" + DECIMAL_FORMAT.format(receipt.getMemberShipDiscountPrice()));
        System.out.println("내실돈\t\t\t\t\t\t" + DECIMAL_FORMAT.format(receipt.getFinalPrice()));
    }

    public void showCurrentStatus(Map<String, Product> generalProductInventory, Map<String, Product> eventProductInventory) {
        System.out.println("안녕하세요. W편의점입니다.");
        System.out.println("현재 보유하고 있는 상품입니다.");
        System.out.println();
        ProductType[] productTypes = ProductType.values();
        for (ProductType productType : productTypes) {
            displayEventStock(eventProductInventory, productType);
            displayGeneralStock(generalProductInventory, productType);
        }
        System.out.println();
    }

    private void displayEventStock(Map<String, Product> eventProductInventory, ProductType eventProductType) {
        Product product = eventProductInventory.get(eventProductType.getName());
        if (product == null) {
            return;
        }
        Integer eventStock = product.getQuantity();

        String formattedPrice = DECIMAL_FORMAT.format(eventProductType.getPrice());
        showEventProductDetailsOrOutOfStock(eventProductType, formattedPrice, eventStock);
    }

    private void showEventProductDetailsOrOutOfStock(ProductType eventProductType, String formattedPrice, Integer eventStock) {
        if (eventStock == EMPTY_STOCK) {
            String eventProductDetails = "-" + " " + eventProductType.getName() + " " + formattedPrice + "원" + " " + "재고 없음" + " " + eventProductType.getPromotion();
            System.out.println(eventProductDetails);
            return;
        }
        String eventProductDetails = "-" + " " + eventProductType.getName() + " " + formattedPrice + "원" + " " + eventStock + "개" + " " + eventProductType.getPromotion();
        System.out.println(eventProductDetails);
    }

    private void showGeneralProductDetailsOrOutOfStock(ProductType generalProductType, String formattedPrice, Integer generalStock) {
        if (generalStock == EMPTY_STOCK) {
            String generalProductDetails = "-" + " " + generalProductType.getName() + " " + formattedPrice + "원" + " " + "재고 없음";
            System.out.println(generalProductDetails);
            return;
        }
        String generalProductDetails = "-" + " " + generalProductType.getName() + " " + formattedPrice + "원" + " " + generalStock + "개";
        System.out.println(generalProductDetails);
    }

    private void displayGeneralStock(Map<String, Product> generalProductInventory, ProductType generalProductType) {
        Product product = generalProductInventory.get(generalProductType.getName());
        Integer generalStock = product.getQuantity();
        String formattedPrice = DECIMAL_FORMAT.format(generalProductType.getPrice());
        showGeneralProductDetailsOrOutOfStock(generalProductType, formattedPrice, generalStock);
    }

    public void showReceipt(Receipt receipt, String membershipChoice) {
        System.out.println("===========W 편의점=============");
        System.out.println("상품명\t\t\t수량\t\t\t금액");
        displayOrderedProductsDetails(receipt);
        String totalPrice = DECIMAL_FORMAT.format(receipt.getTotalPrice());
        displayReceivedPromotionDetails(receipt);
        displayTotalResult(receipt, totalPrice);
    }

    private void displayReceivedPromotionDetails(Receipt receipt) {
        System.out.println("===========증\t정=============");
        List<PromotionItem> promotionItems = receipt.getPromotionItems();
        for (PromotionItem promotionItem : promotionItems) {
            System.out.println(promotionItem.getName() + "\t\t\t" + promotionItem.getQuantity());
        }
    }

    public void displayErrorMessage(String message) {
        System.out.println(message);
    }
}

package store.view;

import camp.nextstep.edu.missionutils.Console;

public class InputView {
    public String promptOrderItems() {
        System.out.println("구매하실 상품명과 수량을 입력해 주세요. (예: [사이다-2], [감자칩-1])");
        return Console.readLine();
    }

    public String promptForMembershipChoice() {
        System.out.println("멤버십 할인을 받으시겠습니까? (Y/N)");
        return Console.readLine();
    }

    public String promptForContinueShopping() {
        System.out.println("감사합니다. 구매하고 싶은 다른 상품이 있나요? (Y/N)");
        return Console.readLine();
    }

    public String promptForAdditionalQuantity(String productName, int promotionalQuantity) {
        System.out.println("현재" + productName + "은(는)" + promotionalQuantity + "개를 무료로 더 받을 수 있습니다. 추가하시겠습니까? (Y/N)");
        return Console.readLine();
    }

    public String promptForNonPromotionalPurchase(int leftRequestOrderProductQuantity, String requestOrderProductName) {
        System.out.println("현재" + requestOrderProductName + leftRequestOrderProductQuantity + "개는 프로모션 할인이 적용되지 않습니다. 그래도 구매하시겠습니까? (Y/N)");
        return Console.readLine();
    }
}

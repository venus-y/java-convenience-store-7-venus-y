package store;

public class Separator {
    private static final String DEFAULT_DELIMITER = ",";
    private static final String ORDER_DETAILS_PREFIX = "[";
    private static final String ORDER_DETAILS_DELIMITER = "-";
    private static final String ORDER_DETAILS_SUFFIX = "]";
    private static final int INCREMENT_STEP = 1;

    public static String[] separate(String input) {
        return input.split(DEFAULT_DELIMITER);
    }

    public static String separateProductName(String product) {
        int orderDetailsPrefixPos = product.indexOf(ORDER_DETAILS_PREFIX);
        int orderDetailsDelimiterPos = product.indexOf(ORDER_DETAILS_DELIMITER);
        return product.substring(orderDetailsPrefixPos + INCREMENT_STEP, orderDetailsDelimiterPos);
    }

    public static String separateProductQuantity(String product) {
        int orderDetailsDelimiterPos = product.indexOf(ORDER_DETAILS_DELIMITER);
        int orderDetailsSuffixPos = product.indexOf(ORDER_DETAILS_SUFFIX);
        return product.substring(orderDetailsDelimiterPos + INCREMENT_STEP, orderDetailsSuffixPos);
    }

}

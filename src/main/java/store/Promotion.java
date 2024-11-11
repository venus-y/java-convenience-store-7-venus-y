package store;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static store.constant.ProductField.NAME;
import static store.constant.PromotionField.BUY;
import static store.constant.PromotionField.END_DATE;
import static store.constant.PromotionField.GET;
import static store.constant.PromotionField.START_DATE;

public class Promotion {
    private String name;
    private int buy;
    private int get;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Promotion(String name, int buy, int get, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.buy = buy;
        this.get = get;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Promotion fromPromotionDetails(String[] promotionDetails) {
        final int DAY_INCREMENT = 1;
        final int END_OF_DAY_OFFSET = 1;

        return new Promotion(promotionDetails[NAME.getIndex()], Integer.parseInt(promotionDetails[BUY.getIndex()]),
                Integer.parseInt(promotionDetails[GET.getIndex()]), LocalDate.parse(promotionDetails[START_DATE.getIndex()]).atStartOfDay(),
                LocalDate.parse(promotionDetails[END_DATE.getIndex()]).plusDays(DAY_INCREMENT).atStartOfDay().minusSeconds(END_OF_DAY_OFFSET));
    }

    public String getName() {
        return name;
    }

    public int getBuy() {
        return buy;
    }

    public int getGet() {
        return get;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}

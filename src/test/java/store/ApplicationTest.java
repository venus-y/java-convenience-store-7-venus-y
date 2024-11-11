package store;

import camp.nextstep.edu.missionutils.test.NsTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static camp.nextstep.edu.missionutils.test.Assertions.assertNowTest;
import static camp.nextstep.edu.missionutils.test.Assertions.assertSimpleTest;
import static org.assertj.core.api.Assertions.assertThat;
import static store.constant.ErrorMessage.EXCEEDS_AVAILABLE_STOCK;
import static store.constant.ErrorMessage.INVALID_INPUT;
import static store.constant.ErrorMessage.INVALID_ORDER_PRODUCT_FORMAT;
import static store.constant.ErrorMessage.INVALID_QUANTITY_ZERO;
import static store.constant.ErrorMessage.PRODUCT_NOT_FOUND;

class ApplicationTest extends NsTest {
    @Test
    void 파일에_있는_상품_목록_출력() {
        assertSimpleTest(() -> {
            run("[물-1]", "N", "N");
            assertThat(output()).contains(
                    "- 콜라 1,000원 10개 탄산2+1",
                    "- 콜라 1,000원 10개",
                    "- 사이다 1,000원 8개 탄산2+1",
                    "- 사이다 1,000원 7개",
                    "- 오렌지주스 1,800원 9개 MD추천상품",
                    "- 오렌지주스 1,800원 재고 없음",
                    "- 탄산수 1,200원 5개 탄산2+1",
                    "- 탄산수 1,200원 재고 없음",
                    "- 물 500원 10개",
                    "- 비타민워터 1,500원 6개",
                    "- 감자칩 1,500원 5개 반짝할인",
                    "- 감자칩 1,500원 5개",
                    "- 초코바 1,200원 5개 MD추천상품",
                    "- 초코바 1,200원 5개",
                    "- 에너지바 2,000원 5개",
                    "- 정식도시락 6,400원 8개",
                    "- 컵라면 1,700원 1개 MD추천상품",
                    "- 컵라면 1,700원 10개"
            );
        });
    }

    @Test
    void 여러_개의_일반_상품_구매() {
        assertSimpleTest(() -> {
            run("[비타민워터-3],[물-2],[정식도시락-2]", "N", "N");
            assertThat(output().replaceAll("\\s", "")).contains("내실돈18,300");
        });
    }

    @Test
    void 기간에_해당하지_않는_프로모션_적용() {
        assertNowTest(() -> {
            run("[감자칩-2]", "N", "N");
            assertThat(output().replaceAll("\\s", "")).contains("내실돈3,000");
        }, LocalDate.of(2024, 2, 1).atStartOfDay());
    }

    @Test
    void 프로모션_상품을_추가로_받는_테스트() {
        assertSimpleTest(() -> {
            runException("[콜라-5]", "Y", "Y");
            assertThat(output()).contains("행사할인\t\t\t\t\t\t -2,000", "총구매액\t\t\t\t6\t\t6,000", "콜라                6       6000", "내실돈\t\t\t\t\t\t4,000");
        });
    }

    @Test
    @DisplayName("추가 구매 요청 시 이전 주문으로 인해 갱신된 재고가 카탈로그에 반영되었는지 검증")
    void 재고_갱신여부_검증() {
        assertSimpleTest(() -> {
            runException("[콜라-6]", "Y", "Y", "Y");
            assertThat(output()).contains("콜라 1,000원 4개 탄산2+1");
        });
    }


    @Test
    @DisplayName("콜라 5개 구매 후, 11개를 재구매하는데 프로모션 수량은 부족한 경우 나머지는 정가로 결제하는 테스트")
    void 두번에_걸친_구매_요청() {
        assertSimpleTest(() -> {
            runException("[콜라-5]", "Y", "Y", "Y", "[콜라-11]", "Y", "Y");
            assertThat(output().replaceAll("\\s", "")).contains("내실돈4,000", "내실돈7,600");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"[콜라-25]", "[정식도시락-10]"})
    @DisplayName("이벤트 상품의 경우, 이벤트재고 + 일반 재고 < 요청수량, 일반 상품의 경우 일반 재고 < 요청수량임을 검증")
    void 상품_종류에_따른_재고_초과_테스트(String orderItems) {
        assertSimpleTest(() -> {
            runException(orderItems);
            assertThat(output()).contains(EXCEEDS_AVAILABLE_STOCK.getValue());
        });
    }

    @Test
    @DisplayName("콜라 5개를 구매 후, 11개를 재구매하는데 프로모션이 적용되지 않는 수량은 제외하고 결제하는 테스트")
    void 프로모션_적용_수량만_구매() {
        assertSimpleTest(() -> {
            runException("[콜라-5]", "Y", "Y", "Y", "[콜라-11]", "Y", "N");
            assertThat(output().replaceAll("\\s", "")).contains("내실돈4,000", "내실돈2,000");
        });
    }

    @Test
    void 멤버십_사용여부에_잘못된_값_입력() {
        assertSimpleTest(() -> {
            runException("[콜라-5]", "dddd");
            assertThat(output()).contains(INVALID_INPUT.getValue());
        });
    }

    @Test
    void 재고보다_많은_구매요청() {
        assertSimpleTest(() -> {
            runException("[콜라-25]");
            assertThat(output()).contains(EXCEEDS_AVAILABLE_STOCK.getValue());
        });
    }

    @Test
    void 추가구매_의사에_잘못된_값_입력() {
        assertSimpleTest(() -> {
            runException("[콜라-5]", "Y", "Y", "example");
            assertThat(output()).contains(INVALID_INPUT.getValue());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"[혹시이거있나요-2],[그럼이건요-3],[그럼이것도없나요-4]"})
    void 상품_목록에_없는_상품을_입력했을_경우(String inputOrderProducts) {
        assertSimpleTest(() -> {
            runException(inputOrderProducts);
            assertThat(output()).contains(PRODUCT_NOT_FOUND.getValue());
        });
    }

    @Test
    void 구매수량을_0개_이하로_입력() {
        assertSimpleTest(() -> {
            runException("[컵라면-0]");
            assertThat(output()).contains(INVALID_QUANTITY_ZERO.getValue());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"[예외케이스%%%%-2],[안녕하세요89489489],[테스테스테스-///]"})
    void 구매요청_형식을_준수하지_않은_경우(String inputOrderProducts) {
        assertSimpleTest(() -> {
            runException(inputOrderProducts);
            assertThat(output()).contains(INVALID_ORDER_PRODUCT_FORMAT.getValue());
        });
    }


    @Test
    void 예외_테스트() {
        assertSimpleTest(() -> {
            runException("[컵라면-12]", "N", "N");
            assertThat(output()).contains("[ERROR] 재고 수량을 초과하여 구매할 수 없습니다. 다시 입력해 주세요.");
        });
    }

    @Override
    public void runMain() {
        Application.main(new String[]{});
    }
}

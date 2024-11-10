# ✏️ 기능 목록

###  - 상품 목록과 프로모션 정보를 불러오기

###  - 구매할 상품 입력받기

###  - 프로모션 적용 가능 시 적용 또는 미적용에 대한 희망 여부를 입력받기

###  - 프로모션 적용 대상 상품 중 일부 수량을 프로모션 없이 결제할 경우, 정가로 결제할 것인지 해당 수량은 구매하지 않을 것인지에 대한 희망 여부를 입력받기

###  - 멤버십 할인 적용 여부 입력받기

###  - 영수증을 통해 구매 상품 내역, 증정 상품 내역, 금액 정보 출력하기

###  - 추가 구매에 대한 희망 여부를 입력받기
---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# 세부 구현 계획
 
#### StoreController : 편의점 프로그램의 흐름을 제어

#### ErrorMessage, ProductField, ProductType, PromotionField, Regex : 편의점 프로그램에서 사용되는 값들을 ENUM으로 정의함 

#### LottoController : 로또 프로그램의 전반적인 흐름을 제어

#### InputView : 사용자로부터 값을 입력받는 역할을 담당

#### OutputView : 값을 출력하는 역할을 담당

#### OrderItem : 주문상품에 대한 정보를 담은 객체

#### Product : 상품정보를 담은 객체

#### Promotion : 프로모션 정보를 담은 객체

#### PromotionItem : 프로모션을 통해 증정받은 상품의 정보를 담은 객체

#### Receipt : 영수증에 대한 정보를 담은 객체

#### Separator : 입력받은 값을 구분자를 기준으로 분할하는 역할을 담당

#### StoreManager : 상품 구매, 재고 갱신, 영수증을 출력하는 역할을 담당
---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# 예외 상황 정의

#### - 상품 목록에 없는 상품을 입력했을 경우

#### - 주문할 상품 입력 시 요구되는 형식을 지키지 않았을 경우

#### - 구매수량이 0 이하일 경우

#### - 예, 아니오를 의미하는 Y, N만을 입력받을 수 있는 상황에서 다른 값을 입력하였을 경우




    




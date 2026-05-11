package com.example.allinmarket.chat.tool;

import com.example.allinmarket.chat.client.ApiServerClient;
import com.example.allinmarket.common.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatToolsTest {

    @Mock
    private ApiServerClient apiServerClient;

    @InjectMocks
    private ChatTools chatTools;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentToken).thenReturn(TOKEN);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ────────────────────────────────────────────────────
    // getOrders
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrders")
    class GetOrdersTest {

        @Test
        @DisplayName("주문 목록 조회 시 ApiServerClient에 토큰을 전달하고 결과를 반환한다")
        void getOrders_성공() {
            given(apiServerClient.getOrders(TOKEN)).willReturn("{\"orders\": []}");

            String result = chatTools.getOrders();

            assertThat(result).isEqualTo("{\"orders\": []}");
            verify(apiServerClient).getOrders(TOKEN);
        }
    }

    // ────────────────────────────────────────────────────
    // getOrder
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrder")
    class GetOrderTest {

        @Test
        @DisplayName("주문 단건 조회 시 orderId와 토큰을 전달하고 결과를 반환한다")
        void getOrder_성공() {
            given(apiServerClient.getOrder(TOKEN, 42L)).willReturn("{\"orderId\": 42}");

            String result = chatTools.getOrder(42L);

            assertThat(result).isEqualTo("{\"orderId\": 42}");
            verify(apiServerClient).getOrder(TOKEN, 42L);
        }

        @Test
        @DisplayName("orderId가 null이어도 ApiServerClient에 위임한다")
        void getOrder_null_orderId() {
            given(apiServerClient.getOrder(TOKEN, null)).willReturn("{}");

            String result = chatTools.getOrder(null);

            assertThat(result).isEqualTo("{}");
        }
    }

    // ────────────────────────────────────────────────────
    // searchProducts
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchProducts")
    class SearchProductsTest {

        @Test
        @DisplayName("키워드 상품 검색 시 keyword와 토큰을 전달하고 결과를 반환한다")
        void searchProducts_성공() {
            given(apiServerClient.getProducts(TOKEN, "노트북")).willReturn("{\"products\": []}");

            String result = chatTools.searchProducts("노트북");

            assertThat(result).isEqualTo("{\"products\": []}");
            verify(apiServerClient).getProducts(TOKEN, "노트북");
        }

        @Test
        @DisplayName("빈 키워드도 ApiServerClient에 위임한다")
        void searchProducts_빈_키워드() {
            given(apiServerClient.getProducts(TOKEN, "")).willReturn("{\"products\": []}");

            String result = chatTools.searchProducts("");

            assertThat(result).isEqualTo("{\"products\": []}");
        }
    }

    // ────────────────────────────────────────────────────
    // getProduct
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProduct")
    class GetProductTest {

        @Test
        @DisplayName("상품 단건 조회 시 productId와 토큰을 전달하고 결과를 반환한다")
        void getProduct_성공() {
            given(apiServerClient.getProduct(TOKEN, 99L)).willReturn("{\"productId\": 99}");

            String result = chatTools.getProduct(99L);

            assertThat(result).isEqualTo("{\"productId\": 99}");
            verify(apiServerClient).getProduct(TOKEN, 99L);
        }
    }

    // ────────────────────────────────────────────────────
    // createRefund
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("createRefund")
    class CreateRefundTest {

        @Test
        @DisplayName("반품 신청 시 orderId, reason, description과 토큰을 전달하고 결과를 반환한다")
        void createRefund_성공() {
            given(apiServerClient.createRefund(TOKEN, 1L, "CHANGE_OF_MIND", "단순 변심"))
                    .willReturn("{\"refundId\": 1}");

            String result = chatTools.createRefund(1L, "CHANGE_OF_MIND", "단순 변심");

            assertThat(result).isEqualTo("{\"refundId\": 1}");
            verify(apiServerClient).createRefund(TOKEN, 1L, "CHANGE_OF_MIND", "단순 변심");
        }

        @Test
        @DisplayName("description이 null이어도 ApiServerClient에 위임한다")
        void createRefund_description_null() {
            given(apiServerClient.createRefund(TOKEN, 1L, "DAMAGED", null))
                    .willReturn("{\"refundId\": 2}");

            String result = chatTools.createRefund(1L, "DAMAGED", null);

            assertThat(result).isEqualTo("{\"refundId\": 2}");
        }

        @Test
        @DisplayName("reason이 WRONG_ITEM인 경우도 정상 처리된다")
        void createRefund_reason_WRONG_ITEM() {
            given(apiServerClient.createRefund(TOKEN, 5L, "WRONG_ITEM", "다른 상품 도착"))
                    .willReturn("{\"refundId\": 5}");

            String result = chatTools.createRefund(5L, "WRONG_ITEM", "다른 상품 도착");

            assertThat(result).isEqualTo("{\"refundId\": 5}");
        }
    }
}
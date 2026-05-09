package com.example.allinmarket.chat.client;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ApiServerClientTest {

    @Mock
    private RestClient apiServerRestClient;

    @InjectMocks
    private ApiServerClient apiServerClient;

    // ────────────────────────────────────────────────────
    // RestClient GET 체인 목 헬퍼
    //
    // uri()는 String, (String, Object...), Function 등 여러 오버로딩이 있어서
    // 실제 호출과 stub이 불일치하는 경우가 생깁니다.
    // lenient()로 불필요한 stub 경고를 방지합니다.
    // ────────────────────────────────────────────────────

    private void mockGetChain(String token, Object bodyResult) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(apiServerRestClient.get()).willReturn(uriSpec);
        // uri() 오버로딩이 다양하므로 lenient로 모두 stub
        lenient().when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        lenient().when(uriSpec.uri(anyString(), any(Object.class))).thenReturn(headersSpec);
        lenient().when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn(headersSpec);
        given(headersSpec.header(anyString(), anyString())).willReturn(headersSpec);
        given(headersSpec.retrieve()).willReturn(responseSpec);

        if (bodyResult instanceof Throwable t) {
            given(responseSpec.body(String.class)).willThrow(t);
        } else {
            given(responseSpec.body(String.class)).willReturn((String) bodyResult);
        }
    }

    // ────────────────────────────────────────────────────
    // RestClient POST 체인 목 헬퍼
    //
    // body()에 실제 RefundRequest 객체가 전달되므로
    // any()로 매처를 넓혀야 PotentialStubbingProblem을 피할 수 있습니다.
    // ────────────────────────────────────────────────────

    private void mockPostChainThrows(Throwable throwable) {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(apiServerRestClient.post()).willReturn(uriSpec);
        given(uriSpec.uri(anyString(), any(Object.class))).willReturn(bodySpec);
        given(bodySpec.header(anyString(), anyString())).willReturn(bodySpec);
        given(bodySpec.contentType(any())).willReturn(bodySpec);
        given(bodySpec.body(any(Object.class))).willReturn(bodySpec); // any()로 매처 확장
        given(bodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(String.class)).willThrow(throwable);
    }

    private void mockPostChainReturns(String result) {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(apiServerRestClient.post()).willReturn(uriSpec);
        given(uriSpec.uri(anyString(), any(Object.class))).willReturn(bodySpec);
        given(bodySpec.header(anyString(), anyString())).willReturn(bodySpec);
        given(bodySpec.contentType(any())).willReturn(bodySpec);
        given(bodySpec.body(any(Object.class))).willReturn(bodySpec); // any()로 매처 확장
        given(bodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(String.class)).willReturn(result);
    }

    // ────────────────────────────────────────────────────
    // getOrders
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrders")
    class GetOrdersTest {

        @Test
        @DisplayName("정상 응답을 반환한다")
        void getOrders_성공() {
            mockGetChain("Bearer token", "{\"orders\": []}");

            String result = apiServerClient.getOrders("Bearer token");

            assertThat(result).isEqualTo("{\"orders\": []}");
        }

        @Test
        @DisplayName("4xx 응답이면 INVALID_INPUT 예외를 던진다")
        void getOrders_4xx_예외() {
            mockGetChain("Bearer token", HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "", null, null, null));

            assertThatThrownBy(() -> apiServerClient.getOrders("Bearer token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INVALID_INPUT.getMessage());
        }

        @Test
        @DisplayName("5xx 응답이면 INTERNAL_SERVER_ERROR 예외를 던진다")
        void getOrders_5xx_예외() {
            mockGetChain("Bearer token", HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "", null, null, null));

            assertThatThrownBy(() -> apiServerClient.getOrders("Bearer token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INTERNAL_SERVER_ERROR.getMessage());
        }

        @Test
        @DisplayName("연결 실패면 INTERNAL_SERVER_ERROR 예외를 던진다")
        void getOrders_연결_실패() {
            mockGetChain("Bearer token", new RestClientException("Connection refused"));

            assertThatThrownBy(() -> apiServerClient.getOrders("Bearer token"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    // ────────────────────────────────────────────────────
    // getOrder
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrder")
    class GetOrderTest {

        @Test
        @DisplayName("정상 응답을 반환한다")
        void getOrder_성공() {
            mockGetChain("Bearer token", "{\"orderId\": 1}");

            String result = apiServerClient.getOrder("Bearer token", 1L);

            assertThat(result).isEqualTo("{\"orderId\": 1}");
        }

        @Test
        @DisplayName("4xx 응답이면 INVALID_INPUT 예외를 던진다")
        void getOrder_4xx_예외() {
            mockGetChain("Bearer token", HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "", null, null, null));

            assertThatThrownBy(() -> apiServerClient.getOrder("Bearer token", 1L))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INVALID_INPUT.getMessage());
        }

        @Test
        @DisplayName("5xx 응답이면 INTERNAL_SERVER_ERROR 예외를 던진다")
        void getOrder_5xx_예외() {
            mockGetChain("Bearer token", HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "", null, null, null));

            assertThatThrownBy(() -> apiServerClient.getOrder("Bearer token", 1L))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    // ────────────────────────────────────────────────────
    // getProducts
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProducts")
    class GetProductsTest {

        @Test
        @DisplayName("정상 응답을 반환한다")
        void getProducts_성공() {
            mockGetChain("Bearer token", "{\"products\": []}");

            String result = apiServerClient.getProducts("Bearer token", "노트북");

            assertThat(result).isEqualTo("{\"products\": []}");
        }

        @Test
        @DisplayName("4xx 응답이면 INVALID_INPUT 예외를 던진다")
        void getProducts_4xx_예외() {
            mockGetChain("Bearer token", HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "", null, null, null));

            assertThatThrownBy(() -> apiServerClient.getProducts("Bearer token", "노트북"))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INVALID_INPUT.getMessage());
        }
    }

    // ────────────────────────────────────────────────────
    // createRefund
    // ────────────────────────────────────────────────────

    @Nested
    @DisplayName("createRefund")
    class CreateRefundTest {

        @Test
        @DisplayName("정상 반품 신청이 성공한다")
        void createRefund_성공() {
            mockPostChainReturns("{\"refundId\": 1}");

            String result = apiServerClient.createRefund(
                    "Bearer token", 1L, "CHANGE_OF_MIND", "단순 변심");

            assertThat(result).isEqualTo("{\"refundId\": 1}");
        }

        @Test
        @DisplayName("orderId가 null이면 IllegalArgumentException을 던진다")
        void createRefund_orderId_null() {
            assertThatThrownBy(() ->
                    apiServerClient.createRefund("Bearer token", null, "CHANGE_OF_MIND", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("orderId must be positive");
        }

        @Test
        @DisplayName("orderId가 0이면 IllegalArgumentException을 던진다")
        void createRefund_orderId_zero() {
            assertThatThrownBy(() ->
                    apiServerClient.createRefund("Bearer token", 0L, "CHANGE_OF_MIND", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("orderId must be positive");
        }

        @Test
        @DisplayName("orderId가 음수이면 IllegalArgumentException을 던진다")
        void createRefund_orderId_음수() {
            assertThatThrownBy(() ->
                    apiServerClient.createRefund("Bearer token", -1L, "CHANGE_OF_MIND", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("orderId must be positive");
        }

        @Test
        @DisplayName("reason이 null이면 IllegalArgumentException을 던진다")
        void createRefund_reason_null() {
            assertThatThrownBy(() ->
                    apiServerClient.createRefund("Bearer token", 1L, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("reason must not be blank");
        }

        @Test
        @DisplayName("reason이 빈 문자열이면 IllegalArgumentException을 던진다")
        void createRefund_reason_빈_문자열() {
            assertThatThrownBy(() ->
                    apiServerClient.createRefund("Bearer token", 1L, "", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("reason must not be blank");
        }

        @Test
        @DisplayName("description은 null이어도 정상 동작한다")
        void createRefund_description_null_허용() {
            mockPostChainReturns("{\"refundId\": 1}");

            String result = apiServerClient.createRefund(
                    "Bearer token", 1L, "DAMAGED", null);

            assertThat(result).isEqualTo("{\"refundId\": 1}");
        }

        @Test
        @DisplayName("4xx 응답이면 INVALID_INPUT 예외를 던진다")
        void createRefund_4xx_예외() {
            mockPostChainThrows(HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "", null, null, null));

            assertThatThrownBy(() ->
                    apiServerClient.createRefund("Bearer token", 1L, "CHANGE_OF_MIND", null))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INVALID_INPUT.getMessage());
        }

        @Test
        @DisplayName("5xx 응답이면 INTERNAL_SERVER_ERROR 예외를 던진다")
        void createRefund_5xx_예외() {
            mockPostChainThrows(HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "", null, null, null));

            assertThatThrownBy(() ->
                    apiServerClient.createRefund("Bearer token", 1L, "CHANGE_OF_MIND", null))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INTERNAL_SERVER_ERROR.getMessage());
        }

        @Test
        @DisplayName("연결 실패면 INTERNAL_SERVER_ERROR 예외를 던진다")
        void createRefund_연결_실패() {
            mockPostChainThrows(new RestClientException("Connection refused"));

            assertThatThrownBy(() ->
                    apiServerClient.createRefund("Bearer token", 1L, "CHANGE_OF_MIND", null))
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.INTERNAL_SERVER_ERROR.getMessage());
        }
    }
}
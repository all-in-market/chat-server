package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RealtimeChatProviderTest {

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private RealtimeChatProvider chatProvider;

    @Test
    void 구매자_이름_조회_성공() {
        Buyer buyer = Buyer.of(
                "buyer@test.com",
                "12345678",
                "이름",
                "010-1234-5678"
        );
        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(buyer, "name", "이름");

        given(buyerRepository.findById(1L)).willReturn(Optional.of(buyer));

        String name = chatProvider.getUserName(1L, RealtimeChatSenderType.BUYER);

        assertThat(name).isEqualTo("이름");
    }

    @Test
    void 판매자_이름_조회_성공() {
        Seller seller = Seller.of(
                "seller@test.com",
                "12345678",
                "판매자",
                "010-1234-5648",
                "상점이름",
                "123-45-67890",
                "1234-1234"
        );
        ReflectionTestUtils.setField(seller, "id", 2L);
        ReflectionTestUtils.setField(seller, "storeName", "상점이름");

        given(sellerRepository.findById(2L)).willReturn(Optional.of(seller));

        String name = chatProvider.getUserName(2L, RealtimeChatSenderType.SELLER);

        assertThat(name).isEqualTo("상점이름");
    }

    @Test
    void 구매자_없을때_예외발생() {
        given(buyerRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatProvider.getUserName(99L, RealtimeChatSenderType.BUYER))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.BUYER_NOT_FOUND.getMessage());
    }

    @Test
    void 판매자_없을때_예외발생() {
        given(sellerRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatProvider.getUserName(99L, RealtimeChatSenderType.SELLER))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.SELLER_NOT_FOUND.getMessage());
    }

    @Test
    void 여러_구매자_이름_조회_성공() {
        Buyer buyer = Buyer.of(
                "buyer@test.com",
                "12345678",
                "이름",
                "010-1234-5678"
        );
        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(buyer, "name", "이름");

        Buyer buyer2 = Buyer.of(
                "buyer2@test.com",
                "12345678",
                "이름2",
                "010-1234-5678"
        );
        ReflectionTestUtils.setField(buyer2, "id", 2L);
        ReflectionTestUtils.setField(buyer2, "name", "이름2");

        given(buyerRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(buyer, buyer2));

        Map<Long, String> names = chatProvider.getUserNames(List.of(1L, 2L), RealtimeChatSenderType.BUYER);

        assertThat(names).containsEntry(1L, "이름").containsEntry(2L, "이름2");
    }

    @Test
    void 여러_판매자_이름_조회_성공() {
        Seller seller = Seller.of(
                "seller@test.com",
                "12345678",
                "판매자",
                "010-1234-5648",
                "상점이름",
                "123-45-67890",
                "1234-1234"
        );
        ReflectionTestUtils.setField(seller, "id", 10L);
        ReflectionTestUtils.setField(seller, "storeName", "상점이름");

        Seller seller2 = Seller.of(
                "seller2@test.com",
                "12345678",
                "판매자2",
                "010-1234-5648",
                "상점이름2",
                "123-45-67890",
                "1234-1234"
        );
        ReflectionTestUtils.setField(seller2, "id", 20L);
        ReflectionTestUtils.setField(seller2, "storeName", "상점이름2");

        given(sellerRepository.findAllById(List.of(10L, 20L)))
                .willReturn(List.of(seller, seller2));

        Map<Long, String> names = chatProvider.getUserNames(List.of(10L, 20L), RealtimeChatSenderType.SELLER);

        assertThat(names).containsEntry(10L, "상점이름").containsEntry(20L, "상점이름2");
    }
}


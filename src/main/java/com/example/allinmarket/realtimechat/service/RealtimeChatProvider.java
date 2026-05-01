package com.example.allinmarket.realtimechat.service;

import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeChatProvider {
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;

    public String getUserName(Long userId, RealtimeChatSenderType senderType) {
        return switch (senderType) {
            case BUYER -> buyerRepository.findById(userId).orElseThrow(
                    () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
            ).getName();

            case SELLER -> sellerRepository.findById(userId).orElseThrow(
                    () -> new BaseException(ErrorEnum.SELLER_NOT_FOUND)
            ).getStoreName();
        };
    }
}

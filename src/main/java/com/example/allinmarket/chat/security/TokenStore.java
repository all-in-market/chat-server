package com.example.allinmarket.chat.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.chat.consts.ChatConsts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenStore {

    private final StringRedisTemplate stringRedisTemplate;

    public void save(String sessionKey, String token) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(ChatConsts.TOKEN_KEY_PREFIX + sessionKey, token, ChatConsts.TOKEN_TTL);
        } catch (Exception e) {
            log.error("[TokenStore] 토큰 저장 실패", e);
            throw new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR);
        }
    }

    public String get(String sessionKey) {
        try {
            String token = stringRedisTemplate.opsForValue()
                    .get(ChatConsts.TOKEN_KEY_PREFIX + sessionKey);
            if (token == null) {
                throw new BaseException(ErrorEnum.TOKEN_INVALID);
            }
            return token;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TokenStore] 토큰 조회 실패", e);
            throw new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR);
        }
    }

    public void delete(String sessionKey) {
        try {
            stringRedisTemplate.delete(ChatConsts.TOKEN_KEY_PREFIX + sessionKey);
        } catch (Exception e) {
            log.error("[TokenStore] 토큰 삭제 실패", e);
        }
    }
}
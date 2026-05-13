package com.example.allinmarket.common.redis;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisLockAspectTest {

    @Mock
    RedissonClient redissonClient;

    @Mock
    ProceedingJoinPoint joinPoint;

    @Mock
    MethodSignature methodSignature;

    @Mock
    RLock rLock;

    @InjectMocks
    RedisLockAspect redisLockAspect;

    // ── RedisLock 어노테이션 stub ─────────────────────────────────────────────

    /**
     * @RedisLock 어노테이션을 직접 구현한 stub.
     * 테스트마다 key 만 바꿔서 재사용한다.
     */
    private RedisLock lockAnnotation(String key) {
        return new RedisLock() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RedisLock.class;
            }
            @Override public String key()                { return key; }
            @Override public long lockWaitTimeSeconds()  { return 3L; }
            @Override public long lockTimeoutSeconds()   { return 10L; }
            @Override public TimeUnit timeUnit()         { return TimeUnit.SECONDS; }
        };
    }

    /** JoinPoint 공통 stubbing: 파라미터 이름/값 설정 */
    private void stubJoinPoint(String[] paramNames, Object[] args) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);
    }

    // ── 테스트 케이스 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("락 획득 성공 → joinPoint.proceed() 결과 반환 후 락 해제")
    void run_lockAcquired_proceedsAndUnlocks() throws Throwable {
        // given
        stubJoinPoint(new String[]{"roomId"}, new Object[]{1L});
        when(redissonClient.getLock("lock:room:1")).thenReturn(rLock);
        when(rLock.tryLock(3L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        // when
        Object result = redisLockAspect.run(joinPoint, lockAnnotation("'lock:room:' + #roomId"));

        // then
        assertThat(result).isEqualTo("ok");
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("락 획득 실패 → BaseException(REDIS_LOCK_CONFLICT), 락 해제 안 함")
    void run_lockNotAcquired_throwsConflict() throws Throwable {
        // given
        stubJoinPoint(new String[]{"roomId"}, new Object[]{2L});
        when(redissonClient.getLock("lock:room:2")).thenReturn(rLock);
        when(rLock.tryLock(3L, 10L, TimeUnit.SECONDS)).thenReturn(false);

        // when / then
        assertThatThrownBy(() ->
                redisLockAspect.run(joinPoint, lockAnnotation("'lock:room:' + #roomId")))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                        .isEqualTo(ErrorEnum.REDIS_LOCK_CONFLICT));

        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("락 획득 중 InterruptedException → BaseException(REDIS_LOCK_INTERRUPTED), 인터럽트 플래그 복원")
    void run_interrupted_throwsInterrupted() throws Throwable {
        // given
        stubJoinPoint(new String[]{"roomId"}, new Object[]{3L});
        when(redissonClient.getLock("lock:room:3")).thenReturn(rLock);
        when(rLock.tryLock(3L, 10L, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        // when / then
        assertThatThrownBy(() ->
                redisLockAspect.run(joinPoint, lockAnnotation("'lock:room:' + #roomId")))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                        .isEqualTo(ErrorEnum.REDIS_LOCK_INTERRUPTED));

        // 인터럽트 플래그 복원 여부 확인
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted(); // 다른 테스트에 영향 없도록 플래그 초기화
    }

    @Test
    @DisplayName("proceed() 예외 발생 → 예외 전파, 락 해제")
    void run_proceedThrows_exceptionPropagatedAndUnlocked() throws Throwable {
        // given
        stubJoinPoint(new String[]{"roomId"}, new Object[]{4L});
        when(redissonClient.getLock("lock:room:4")).thenReturn(rLock);
        when(rLock.tryLock(3L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("비즈니스 오류"));

        // when / then
        assertThatThrownBy(() ->
                redisLockAspect.run(joinPoint, lockAnnotation("'lock:room:' + #roomId")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("비즈니스 오류");

        // finally 블록에서 락이 해제되어야 함
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("SpEL 키 해석 - 리터럴 + 파라미터 조합이 올바르게 변환됨")
    void resolveKey_spelExpression_resolvedCorrectly() throws Throwable {
        // given
        stubJoinPoint(new String[]{"paymentId"}, new Object[]{99L});
        when(redissonClient.getLock("lock:payment:99")).thenReturn(rLock);
        when(rLock.tryLock(3L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        redisLockAspect.run(joinPoint, lockAnnotation("'lock:payment:' + #paymentId"));

        // then: 올바른 키로 getLock 이 호출됐는지 확인
        verify(redissonClient).getLock("lock:payment:99");
    }
}
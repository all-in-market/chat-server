package com.example.allinmarket.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    @DisplayName("register() - Page 의 content/totalPages/totalElements/size/isLast 가 올바르게 매핑됨")
    void register_mapsPageFieldsCorrectly() {
        // given
        List<String> content = List.of("a", "b", "c");
        // 0-based 첫 번째 페이지, 총 7개 항목, 페이지 크기 3
        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<String> page = new PageImpl<>(content, pageRequest, 7);

        // when
        PageResponse<String> response = PageResponse.register(page);

        // then
        assertThat(response.content()).isEqualTo(content);
        assertThat(response.currentPage()).isEqualTo(1);       // 0-based → 1-based 변환 검증
        assertThat(response.totalPages()).isEqualTo(3);        // ceil(7/3) = 3
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("register() - 마지막 페이지이면 isLast=true")
    void register_lastPage_isLastTrue() {
        // given: 0-based 두 번째(마지막) 페이지, 총 4개, 크기 3 → 마지막 페이지
        List<String> content = List.of("d");
        PageRequest pageRequest = PageRequest.of(1, 3);
        Page<String> page = new PageImpl<>(content, pageRequest, 4);

        // when
        PageResponse<String> response = PageResponse.register(page);

        // then
        assertThat(response.currentPage()).isEqualTo(2);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("register() - 빈 페이지이면 content 가 비어 있고 totalElements=0")
    void register_emptyPage() {
        // given
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        // when
        PageResponse<String> response = PageResponse.register(page);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.isLast()).isTrue();
    }
}
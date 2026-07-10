package com.loopers.domain.queue;

import java.util.Optional;

public interface EntryTokenStore {

    /** userId 에게 입장 토큰을 발급하고, 발급된 토큰 값을 반환한다. TTL 후 자동 만료. */
    String issue(Long userId);

    /** userId 의 현재 유효한 토큰을 조회한다. 없거나 만료됐으면 empty. */
    Optional<String> find(Long userId);

    /** 사용 완료된 토큰을 삭제한다. */
    void remove(Long userId);
}

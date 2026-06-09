package com.girinlog.memo.service;

import org.springframework.stereotype.Component;

@Component
class FixedMemoUserContext implements MemoUserContext {

    private static final Long LOCAL_USER_ID = 1L;

    @Override
    public Long currentUserId() {
        return LOCAL_USER_ID;
    }
}

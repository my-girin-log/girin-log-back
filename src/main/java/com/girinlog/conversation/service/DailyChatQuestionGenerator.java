package com.girinlog.conversation.service;

import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.memo.domain.MemoSummary;

import java.util.List;

public interface DailyChatQuestionGenerator {

    String generateFirstQuestion(List<MemoSummary> memoSummaries);

    String generateFollowUpQuestion(DailyChatSession session);

    /** 사용자 답변 이후, 실록이가 대화를 자연스럽게 종료할지(AI_DECIDED) 판단한다. true면 종료. */
    boolean shouldEnd(DailyChatSession session);

    String generateClosingMessage(DailyChatSession session, EndedReason endedReason);
}

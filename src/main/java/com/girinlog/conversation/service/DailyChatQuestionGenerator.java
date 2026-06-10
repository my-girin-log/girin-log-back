package com.girinlog.conversation.service;

import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.memo.domain.MemoSummary;

import java.util.List;

public interface DailyChatQuestionGenerator {

    String generateFirstQuestion(List<MemoSummary> memoSummaries);

    String generateFollowUpQuestion(DailyChatSession session);

    String generateClosingMessage(DailyChatSession session, EndedReason endedReason);
}

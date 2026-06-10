package com.girinlog.conversation.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.time.ServiceDay;
import com.girinlog.conversation.ConversationErrorCode;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.conversation.repository.DailyChatSessionRepository;
import com.girinlog.memo.domain.MemoSummary;
import com.girinlog.memo.domain.MemoSummaryItem;
import com.girinlog.memo.repository.MemoSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DailyChatSessionService {

    private final DailyChatSessionRepository dailyChatSessionRepository;
    private final MemoSummaryRepository memoSummaryRepository;
    private final DailyChatQuestionGenerator dailyChatQuestionGenerator;
    private final Clock clock;

    public DailyChatSessionService(
            DailyChatSessionRepository dailyChatSessionRepository,
            MemoSummaryRepository memoSummaryRepository,
            DailyChatQuestionGenerator dailyChatQuestionGenerator,
            Clock clock
    ) {
        this.dailyChatSessionRepository = dailyChatSessionRepository;
        this.memoSummaryRepository = memoSummaryRepository;
        this.dailyChatQuestionGenerator = dailyChatQuestionGenerator;
        this.clock = clock;
    }

    @Transactional
    public DailyChatSession createDailyChatSession(Long userId, List<Long> requestedMemoSummaryIds) {
        List<Long> memoSummaryIds = distinctIds(requestedMemoSummaryIds);
        List<MemoSummary> memoSummaries = findSelectedMemoSummaries(userId, memoSummaryIds);
        validateServiceDate(memoSummaries, ServiceDay.today(clock));
        validateChatAvailable(memoSummaries);

        OffsetDateTime now = now();
        String firstQuestion = dailyChatQuestionGenerator.generateFirstQuestion(memoSummaries);
        DailyChatSession session = DailyChatSession.start(
                userId,
                ServiceDay.today(clock),
                memoSummaryIds,
                snapshot(memoSummaries),
                firstQuestion,
                now
        );
        memoSummaries.forEach(MemoSummary::disableChat);
        return dailyChatSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public DailyChatSession getDailyChatSession(Long userId, Long sessionId) {
        return findSession(userId, sessionId);
    }

    @Transactional
    public DailyChatSession submitAnswer(Long userId, Long sessionId, String content) {
        DailyChatSession session = findOpenSession(userId, sessionId);
        OffsetDateTime answeredAt = now();
        session.addUserAnswer(content, answeredAt);

        if (!session.canAskMore()) {
            String closingMessage = dailyChatQuestionGenerator.generateClosingMessage(session, EndedReason.MAX_FOLLOWUP);
            session.end(EndedReason.MAX_FOLLOWUP, closingMessage, answeredAt);
            return session;
        }

        String followUpQuestion = dailyChatQuestionGenerator.generateFollowUpQuestion(session);
        session.addSilokFollowUpQuestion(followUpQuestion, answeredAt);
        return session;
    }

    @Transactional
    public DailyChatSession endSession(Long userId, Long sessionId) {
        DailyChatSession session = findOpenSession(userId, sessionId);
        OffsetDateTime endedAt = now();
        String closingMessage = dailyChatQuestionGenerator.generateClosingMessage(session, EndedReason.USER_ENDED);
        session.end(EndedReason.USER_ENDED, closingMessage, endedAt);
        return session;
    }

    private DailyChatSession findOpenSession(Long userId, Long sessionId) {
        DailyChatSession session = findSession(userId, sessionId);
        if (session.isEnded()) {
            throw new BusinessException(ConversationErrorCode.DAILY_CHAT_SESSION_ALREADY_ENDED);
        }
        return session;
    }

    private DailyChatSession findSession(Long userId, Long sessionId) {
        return dailyChatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ConversationErrorCode.DAILY_CHAT_SESSION_NOT_FOUND));
    }

    private List<MemoSummary> findSelectedMemoSummaries(Long userId, List<Long> memoSummaryIds) {
        List<MemoSummary> memoSummaries = memoSummaryRepository.findByUserIdAndIdIn(userId, memoSummaryIds);
        if (memoSummaries.size() != memoSummaryIds.size()) {
            throw new BusinessException(ConversationErrorCode.MEMO_SUMMARY_NOT_FOUND);
        }
        return sortByRequestOrder(memoSummaries, memoSummaryIds);
    }

    private List<MemoSummary> sortByRequestOrder(List<MemoSummary> memoSummaries, List<Long> memoSummaryIds) {
        List<MemoSummary> sorted = new ArrayList<>();
        for (Long memoSummaryId : memoSummaryIds) {
            sorted.add(memoSummaries.stream()
                    .filter(memoSummary -> memoSummary.id().equals(memoSummaryId))
                    .findFirst()
                    .orElseThrow());
        }
        return sorted;
    }

    private void validateServiceDate(List<MemoSummary> memoSummaries, LocalDate serviceDate) {
        Set<LocalDate> serviceDates = memoSummaries.stream()
                .map(MemoSummary::serviceDate)
                .collect(Collectors.toSet());
        if (serviceDates.size() > 1 || !serviceDates.contains(serviceDate)) {
            throw new BusinessException(ConversationErrorCode.MEMO_SUMMARY_SERVICE_DATE_MISMATCH);
        }
    }

    private void validateChatAvailable(List<MemoSummary> memoSummaries) {
        boolean hasUnavailableSummary = memoSummaries.stream().anyMatch(memoSummary -> !memoSummary.chatAvailable());
        if (hasUnavailableSummary) {
            throw new BusinessException(ConversationErrorCode.MEMO_SUMMARY_NOT_CHAT_AVAILABLE);
        }
    }

    private List<Long> distinctIds(List<Long> memoSummaryIds) {
        return new LinkedHashSet<>(memoSummaryIds).stream().toList();
    }

    private String snapshot(List<MemoSummary> memoSummaries) {
        return memoSummaries.stream()
                .map(this::snapshot)
                .collect(Collectors.joining("\n\n"));
    }

    private String snapshot(MemoSummary memoSummary) {
        String items = memoSummary.items().stream()
                .map(MemoSummaryItem::content)
                .map("- %s"::formatted)
                .collect(Collectors.joining("\n"));
        return """
                memoSummaryId: %d
                categoryName: %s
                summary: %s
                items:
                %s
                """.formatted(memoSummary.id(), memoSummary.categoryName(), memoSummary.summary(), items).strip();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}

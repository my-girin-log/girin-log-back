package com.girinlog.diary.service;

import com.girinlog.conversation.domain.DailyChatSession;

import java.time.LocalDate;
import java.util.List;

public interface DiaryGenerator {

    DiaryContent generate(LocalDate serviceDate, List<DailyChatSession> sessions);
}

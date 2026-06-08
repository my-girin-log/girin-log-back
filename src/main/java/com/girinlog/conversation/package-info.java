/**
 * 대화 도메인 — DailyChatSession. (BE-B 담당)
 *
 * <p>하나 이상의 MemoSummary 선택으로 세션 시작. 하루 여러 세션 가능.
 * 역질문 최대 10회(서버 정책 + 프롬프트 양쪽 보장). 종료 사유는 endedReason.
 * 전체 대화 원문은 별도 ChatMessage 엔티티 없이 conversation 필드에 순서대로 저장한다.
 */
package com.girinlog.conversation;

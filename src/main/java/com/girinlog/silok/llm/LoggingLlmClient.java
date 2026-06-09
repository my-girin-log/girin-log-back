package com.girinlog.silok.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class LoggingLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingLlmClient.class);

    private final LlmClient delegate;
    private final Clock clock;

    public LoggingLlmClient(LlmClient delegate, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate는 필수입니다.");
        this.clock = Objects.requireNonNull(clock, "clock은 필수입니다.");
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        Instant startedAt = Instant.now(clock);
        log.info(
                "silok llm request provider={} model={} timeout={} maxOutputTokens={}",
                request.provider(),
                request.model(),
                request.options().timeout(),
                request.options().maxOutputTokens()
        );

        try {
            LlmResponse response = delegate.generate(request);
            log.info(
                    "silok llm response provider={} model={} elapsedMs={} contentLength={} usageKnown={}",
                    request.provider(),
                    response.model(),
                    elapsedMillisSince(startedAt),
                    response.content().length(),
                    response.usage().known()
            );
            return response;
        } catch (SilokLlmException exception) {
            log.warn(
                    "silok llm failed provider={} model={} elapsedMs={} reason={}",
                    request.provider(),
                    request.model(),
                    elapsedMillisSince(startedAt),
                    exception.reason()
            );
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "silok llm failed provider={} model={} elapsedMs={} reason={}",
                    request.provider(),
                    request.model(),
                    elapsedMillisSince(startedAt),
                    LlmFailureReason.PROVIDER_ERROR
            );
            throw new SilokLlmException(
                    LlmFailureReason.PROVIDER_ERROR,
                    "실록이 LLM 호출에 실패했습니다.",
                    exception
            );
        }
    }

    private long elapsedMillisSince(Instant startedAt) {
        return Duration.between(startedAt, Instant.now(clock)).toMillis();
    }
}

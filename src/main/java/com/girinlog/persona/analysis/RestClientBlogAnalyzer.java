package com.girinlog.persona.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * RestClient 기반 블로그 본문 추출기.
 *
 * <p>임의 URL을 가져오므로 SSRF를 막는다: http(s)만 허용하고, 로컬/사설/링크로컬 호스트는 차단한다.
 * 응답은 태그를 제거해 일정 길이로 자른다. 어떤 실패든 빈 값으로 graceful 처리한다.
 */
@Component
class RestClientBlogAnalyzer implements BlogAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RestClientBlogAnalyzer.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_TEXT_LENGTH = 4_000;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final RestClient restClient;

    RestClientBlogAnalyzer() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) TIMEOUT.toMillis());
        factory.setReadTimeout((int) TIMEOUT.toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public Optional<String> fetchReadableText(String blogUrl) {
        if (!isSafeUrl(blogUrl)) {
            log.info("블로그 URL이 안전하지 않아 분석을 건너뜁니다: {}", blogUrl);
            return Optional.empty();
        }
        try {
            String html = restClient.get().uri(URI.create(blogUrl)).retrieve().body(String.class);
            String text = extractText(html);
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (RuntimeException exception) {
            log.info("블로그 분석 실패({}): {}", blogUrl, exception.getMessage());
            return Optional.empty();
        }
    }

    /** http(s)이고 로컬/사설/링크로컬 호스트가 아닌 경우만 허용(SSRF 방지). */
    static boolean isSafeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                return false;
            }
            InetAddress address = InetAddress.getByName(host);
            return !(address.isLoopbackAddress()
                    || address.isAnyLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress());
        } catch (RuntimeException | java.net.UnknownHostException exception) {
            return false;
        }
    }

    /** script/style 제거 → 태그 제거 → 기본 엔티티 복원 → 공백 정리 → 길이 제한. */
    static String extractText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String withoutBlocks = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        String withoutTags = withoutBlocks.replaceAll("(?s)<[^>]+>", " ");
        String unescaped = withoutTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        String collapsed = unescaped.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= MAX_TEXT_LENGTH) {
            return collapsed;
        }
        return collapsed.substring(0, MAX_TEXT_LENGTH);
    }
}

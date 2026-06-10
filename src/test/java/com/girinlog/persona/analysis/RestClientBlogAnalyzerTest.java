package com.girinlog.persona.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientBlogAnalyzerTest {

    @Test
    @DisplayName("http(s) 외 스킴·로컬/사설 호스트는 차단한다(SSRF)")
    void rejectsUnsafeUrls() {
        assertThat(RestClientBlogAnalyzer.isSafeUrl(null)).isFalse();
        assertThat(RestClientBlogAnalyzer.isSafeUrl("ftp://example.com")).isFalse();
        assertThat(RestClientBlogAnalyzer.isSafeUrl("file:///etc/passwd")).isFalse();
        assertThat(RestClientBlogAnalyzer.isSafeUrl("http://localhost/")).isFalse();
        assertThat(RestClientBlogAnalyzer.isSafeUrl("http://127.0.0.1/")).isFalse();
        assertThat(RestClientBlogAnalyzer.isSafeUrl("http://169.254.169.254/")).isFalse();
        assertThat(RestClientBlogAnalyzer.isSafeUrl("http://192.168.0.1/")).isFalse();
        assertThat(RestClientBlogAnalyzer.isSafeUrl("http://10.0.0.1/")).isFalse();
    }

    @Test
    @DisplayName("공개 http(s) URL은 허용한다")
    void allowsPublicUrls() {
        assertThat(RestClientBlogAnalyzer.isSafeUrl("https://example.com/post/1")).isTrue();
    }

    @Test
    @DisplayName("script/style·태그를 제거하고 공백을 정리한다")
    void extractsReadableText() {
        String html = """
                <html><head><style>.a{color:red}</style></head>
                <body><script>alert(1)</script><h1>제목</h1><p>본문&nbsp;내용 &amp; 더보기</p></body></html>
                """;

        String text = RestClientBlogAnalyzer.extractText(html);

        assertThat(text).contains("제목").contains("본문 내용 & 더보기");
        assertThat(text).doesNotContain("alert").doesNotContain("color:red").doesNotContain("<");
    }

    @Test
    @DisplayName("4000자를 넘으면 자른다")
    void truncatesLongText() {
        String html = "<p>" + "가".repeat(5_000) + "</p>";

        assertThat(RestClientBlogAnalyzer.extractText(html)).hasSize(4_000);
    }
}

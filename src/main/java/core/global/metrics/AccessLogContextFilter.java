package core.global.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.jboss.logging.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccessLogContextFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String u = request.getRequestURI();
        return u.equals("/") || u.startsWith("/health") || u.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        long start = System.nanoTime();

        String rid = Optional.ofNullable(req.getHeader("X-Request-Id"))
                .filter(s -> !s.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        MDC.put("requestId", rid);

        putIfNotBlank("userId", getUserIdSafely());
        MDC.put("method", req.getMethod());
        MDC.put("uri", buildFullUri(req));
        MDC.put("remoteIp", getClientIp(req));

        long in = req.getContentLengthLong();
        if (in >= 0) MDC.put("bytesIn", String.valueOf(in));

        // (선택) bytesOut 측정이 필요하면 Response 래퍼로 감쌉니다.
        CountingHttpServletResponse wrapped = new CountingHttpServletResponse(res);
        try {
            chain.doFilter(req, wrapped);
        } finally {
            MDC.put("status", String.valueOf(wrapped.getStatus()));
            MDC.put("latencyMs", String.valueOf((System.nanoTime() - start) / 1_000_000));

            long out = wrapped.getBodySize();
            if (out >= 0) MDC.put("bytesOut", String.valueOf(out));

            MDC.clear();
        }
    }

    private static void putIfNotBlank(String key, String value) {
        if (value != null && !value.isBlank()) MDC.put(key, value);
    }

    private String buildFullUri(HttpServletRequest req) {
        String qs = req.getQueryString();
        return (qs == null || qs.isBlank())
                ? req.getRequestURI()
                : req.getRequestURI() + "?" + qs;
    }

    private String getClientIp(HttpServletRequest req) {
        // 필요한 경우에만 신뢰하는 프록시 헤더 사용 (LB 앞단에서 sanitize 보장 시)
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return req.getRemoteAddr();
    }

    private String getUserIdSafely() {
        // SecurityContext에서 꺼내되 실패 시 null 반환
        return null;
    }

    /** ---- 선택: 응답 바이트 수 집계용 래퍼 ---- */
    private static class CountingHttpServletResponse extends HttpServletResponseWrapper {
        private ServletOutputStream out;
        private PrintWriter writer;
        private long count = 0;

        CountingHttpServletResponse(HttpServletResponse response) { super(response); }

        @Override public ServletOutputStream getOutputStream() throws IOException {
            if (out == null) {
                out = new ServletOutputStreamWrapper(super.getOutputStream(), this::add);
            }
            return out;
        }
        @Override public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()), true);
            }
            return writer;
        }
        void add(int n) { if (n > 0) count += n; }
        long getBodySize() { return count; }

        private static class ServletOutputStreamWrapper extends ServletOutputStream {
            private final ServletOutputStream delegate;
            private final IntConsumer onWrite;
            ServletOutputStreamWrapper(ServletOutputStream delegate, IntConsumer onWrite) {
                this.delegate = delegate; this.onWrite = onWrite;
            }
            @Override public boolean isReady() { return delegate.isReady(); }
            @Override public void setWriteListener(WriteListener writeListener) { delegate.setWriteListener(writeListener); }
            @Override public void write(int b) throws IOException { delegate.write(b); onWrite.accept(1); }
            @Override public void write(byte[] b, int off, int len) throws IOException { delegate.write(b, off, len); onWrite.accept(len); }
        }
        @FunctionalInterface private interface IntConsumer { void accept(int value); }
    }
}

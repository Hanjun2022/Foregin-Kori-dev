package core.global.config;

import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMailDispatcher {

    private final JavaMailSender mailSender;

    // ✅ SMTP 동시 접속 억제(네이버는 1~2 권장) — 비동기여도 직렬 처리 보장
    private final Semaphore smtpGate = new Semaphore(1);

    @Async("mailExecutor")     // ✅ 메일 전용 풀 사용(다른 @Async 불간섭)
    @Retry(name = "mailSend")
    public void sendHtmlAsync(String from, String to, String subject, String html) {
        try {
            smtpGate.acquire();
            MimeMessage mm = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mm,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(mm);
            log.info("메일 전송 완료(to={}): {}", to, subject);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("MAIL_SEND_INTERRUPTED", e);
        } catch (MailException | MessagingException e) {
            log.error("메일 전송 실패(to={}): {}", to, e.getMessage(), e);
            throw new RuntimeException("MAIL_SEND_FAILED", e);
        } finally {
            smtpGate.release();
        }
    }
}

package core.global.service;

import core.global.config.AsyncMailDispatcher;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpMailService {

    private final MessageSource messageSource;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AsyncMailDispatcher asyncMailDispatcher;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.brand}")
    private String defaultBrand;

    private final Semaphore smtpGate = new Semaphore(1);

    public String sendVerificationEmail(String toEmail, Duration ttl, Locale locale) {
        String code = generateCode();

        String subject = getEmailSubject("password.reset.subject", locale);

        Context ctx = createBaseContext(locale);
        ctx.setVariable("code", code);
        ctx.setVariable("ttlMinutes", ttl.toMinutes());

        String html = templateEngine.process("email/verification", ctx);
        asyncMailDispatcher.sendHtmlAsync(from, toEmail, subject, html);

        log.info("인증 메일 발송 완료: {} (코드: {})", toEmail, code);
        return code;
    }

    public void sendPasswordResetSessionEmail(String toEmail,
                                              Duration sessionTtl,
                                              Locale locale,
                                              String startUrl) {
        String subject = getEmailSubject("email.reset.subject", locale);

        Context ctx = createBaseContext(locale);
        ctx.setVariable("startUrl", startUrl);
        ctx.setVariable("ttlMinutes", sessionTtl.toMinutes());

        String html = templateEngine.process("email/reset-password", ctx);
        asyncMailDispatcher.sendHtmlAsync(from, toEmail, subject, html);
    }

    public void sendPasswordResetEmail(String toEmail, Duration ttl, Locale locale, String resetLink) {
        Locale resolvedLocale = resolveLocale(locale);

        String subject = getEmailSubject("password.reset.subject", resolvedLocale);

        Context ctx = createBaseContext(resolvedLocale);
        ctx.setVariable("ttlMinutes", ttl.toMinutes());
        ctx.setVariable("resetLink", resetLink);

        String html = templateEngine.process("email/reset-password", ctx);
        asyncMailDispatcher.sendHtmlAsync(from, toEmail, subject, html);
    }

    /**
     * 기본 Context 생성 (brand 변수 포함)
     */
    private Context createBaseContext(Locale locale) {
        Context ctx = new Context(locale);
        ctx.setVariable("brand", defaultBrand);
        return ctx;
    }

    /**
     * 이메일 제목 생성 (다국어 처리)
     */
    private String getEmailSubject(String messageKey, Locale locale) {
        return messageSource.getMessage(
                messageKey,
                new Object[]{defaultBrand},
                locale
        );
    }

    /**
     * Locale 해석 (null 처리)
     */
    private Locale resolveLocale(Locale locale) {
        if (locale != null) {
            return locale;
        }
        Locale contextLocale = LocaleContextHolder.getLocale();
        return contextLocale != null ? contextLocale : Locale.getDefault();
    }

    private void sendHtml(String to, String subject, String html) {
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("MAIL_SEND_INTERRUPTED", e);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send mail to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("MAIL_SEND_FAILED", e);
        } finally {
            smtpGate.release();
        }
    }

    private String generateCode() {
        int n = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return Integer.toString(n);
    }
}
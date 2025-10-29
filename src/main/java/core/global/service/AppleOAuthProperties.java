package core.global.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.apple")
public record AppleOAuthProperties(
        String teamId,
        String keyId,
        String clientId,
        String appBundleId,
        String redirectUri,
        String privateKeyPem
) {}

package core.global.config;

import core.global.service.AppleOAuthProperties;
import feign.Logger;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
@EnableConfigurationProperties(AppleOAuthProperties.class)
public class AppleFeignConfig {
    @Bean
    public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> converters) {
        return new SpringFormEncoder(new SpringEncoder(converters));
    }
    @Bean
    Logger.Level feignLoggerLevel() { return Logger.Level.FULL; }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
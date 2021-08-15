package com.ariseontech.joindesk;

import com.ariseontech.joindesk.auth.util.AuditorAwareImpl;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Objects;
import java.util.concurrent.Executor;


@SpringBootApplication(proxyBeanMethods = false)
@EnableJpaRepositories
@EnableJpaAuditing
@EnableCaching
@EnableTransactionManagement
@EnableAutoConfiguration
@EnableScheduling
@EnableAsync
@Log
public class JoindeskApplication implements CommandLineRunner {

    @Autowired
    private SetupInitializer setupInitializer;
    @Autowired
    private CacheManager cacheManager;
    @Value("${data-dir}")
    private String dataPath;

    public static void main(String[] args) {
        SpringApplication.run(JoindeskApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            setupInitializer.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("JDEventService-");
        executor.initialize();
        return executor;
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setValidationMessageSource(messageSource);
        return validatorFactoryBean;
    }

    @Bean
    AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    @Scheduled(fixedRate = 1800000)//every 30 minutes 1800000
    private void resourceUsageLogger() {
        log.info("Clear cache");
        cacheManager.getCacheNames().forEach(c -> {
            log.info("Clearing cache: " + c);
            Objects.requireNonNull(cacheManager.getCache(c)).clear();
        });
        //log.info(new SystemInfo().Info());
    }
}

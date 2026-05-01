package com.thespawnpoint.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitingService {

    private final Cache<String, Bucket> loginBuckets;
    private final Cache<String, Bucket> registerBuckets;
    private final Cache<String, Bucket> forgotPasswordBuckets;
    private final Cache<String, Bucket> chatBuckets;
    private final Cache<String, Bucket> uploadBuckets;
    private final Cache<String, Bucket> defaultBuckets;

    private final Cache<Long, MessageState> lastMessages;

    @Value("${app.rate-limit.login.capacity:5}")
    private int loginCapacity;
    @Value("${app.rate-limit.login.seconds:60}")
    private int loginSeconds;

    @Value("${app.rate-limit.register.capacity:3}")
    private int registerCapacity;
    @Value("${app.rate-limit.register.seconds:3600}")
    private int registerSeconds;

    @Value("${app.rate-limit.forgot-password.capacity:3}")
    private int forgotPasswordCapacity;
    @Value("${app.rate-limit.forgot-password.seconds:3600}")
    private int forgotPasswordSeconds;

    @Value("${app.rate-limit.chat.capacity:10}")
    private int chatCapacity;
    @Value("${app.rate-limit.chat.seconds:5}")
    private int chatSeconds;

    @Value("${app.rate-limit.chat.spam-threshold:3}")
    private int spamThreshold;

    @Value("${app.rate-limit.upload.capacity:5}")
    private int uploadCapacity;
    @Value("${app.rate-limit.upload.seconds:3600}")
    private int uploadSeconds;

    @Value("${app.rate-limit.default.capacity:20}")
    private int defaultCapacity;
    @Value("${app.rate-limit.default.seconds:60}")
    private int defaultSeconds;

    public RateLimitingService() {
        this.loginBuckets          = Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
        this.registerBuckets       = Caffeine.newBuilder().expireAfterAccess(24, TimeUnit.HOURS).build();
        this.forgotPasswordBuckets = Caffeine.newBuilder().expireAfterAccess(24, TimeUnit.HOURS).build();
        this.chatBuckets           = Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
        this.uploadBuckets         = Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
        this.defaultBuckets        = Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
        this.lastMessages          = Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
    }

    public Bucket resolveBucket(String key, String type) {
        return switch (type) {
            case "LOGIN"           -> loginBuckets.get(key, k -> createNewBucket(loginCapacity, loginSeconds));
            case "REGISTER"        -> registerBuckets.get(key, k -> createNewBucket(registerCapacity, registerSeconds));
            case "FORGOT_PASSWORD" -> forgotPasswordBuckets.get(key, k -> createNewBucket(forgotPasswordCapacity, forgotPasswordSeconds));
            case "CHAT"            -> chatBuckets.get(key, k -> createNewBucket(chatCapacity, chatSeconds));
            case "UPLOAD"          -> uploadBuckets.get(key, k -> createNewBucket(uploadCapacity, uploadSeconds));
            default                -> defaultBuckets.get(key, k -> createNewBucket(defaultCapacity, defaultSeconds));
        };
    }

    public boolean isSpamDuplicate(Long userId, String content) {
        if (content == null || content.isBlank()) return false;

        MessageState existing = lastMessages.getIfPresent(userId);

        if (existing != null && content.equals(existing.getContent())) {
            int newCount = existing.incrementAndGet();
            return newCount > spamThreshold;
        } else {
            lastMessages.put(userId, new MessageState(content));
            return false;
        }
    }

    private Bucket createNewBucket(int capacity, int seconds) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofSeconds(seconds)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Getter
    private static class MessageState {
        private final String content;
        private final AtomicInteger count;

        public MessageState(String content) {
            this.content = content;
            this.count = new AtomicInteger(1);
        }

        public int incrementAndGet() {
            return count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }
    }
}
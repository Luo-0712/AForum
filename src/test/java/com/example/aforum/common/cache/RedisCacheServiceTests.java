package com.example.aforum.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTests {

    private static final String KEY = "aforum:cache:v1:test:value";

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CacheProperties properties;
    private ObjectMapper objectMapper;
    private RedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        properties = new CacheProperties();
        objectMapper = new ObjectMapper();
        cacheService = new RedisCacheService(redisTemplateProvider, objectMapper, properties);
    }

    @AfterEach
    void tearDown() {
        org.mockito.Mockito.validateMockitoUsage();
    }

    @Test
    void readValueAndNegativeEntryWithoutExposingCorruptData() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.put(KEY, new Sample(42), Duration.ofSeconds(30));
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq(KEY),
                jsonCaptor.capture(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().toSeconds()).isEqualTo(30);

        when(valueOperations.get(KEY)).thenReturn(jsonCaptor.getValue());
        RedisCacheService.CacheLookup<Sample> hit = cacheService.get(KEY, Sample.class);
        assertThat(hit.hit()).isTrue();
        assertThat(hit.value().value()).isEqualTo(42);

        String negativeJson = "{\"present\":false,\"data\":null}";
        when(valueOperations.get(KEY)).thenReturn(negativeJson);
        RedisCacheService.CacheLookup<Sample> negative = cacheService.get(KEY, Sample.class);
        assertThat(negative.negativeHit()).isTrue();
        assertThat(negative.value()).isNull();
    }

    @Test
    void corruptValueIsTreatedAsMissAndDeleted() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("{not-json");

        RedisCacheService.CacheLookup<Sample> lookup = cacheService.get(KEY, Sample.class);

        assertThat(lookup.value()).isNull();
        assertThat(lookup.hit()).isFalse();
        assertThat(lookup.negativeHit()).isFalse();
        verify(redisTemplate).delete(KEY);
    }

    @Test
    void redisFailureIsAMissAndDoesNotPropagate() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenThrow(new IllegalStateException("redis down"));

        RedisCacheService.CacheLookup<Sample> lookup = cacheService.get(KEY, Sample.class);

        assertThat(lookup.hit()).isFalse();
        assertThat(lookup.negativeHit()).isFalse();
        assertThat(lookup.value()).isNull();
    }

    @Test
    void missingRedisDisablesReads() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);

        assertThat(cacheService.isEnabled()).isFalse();
        assertThat(cacheService.get(KEY, Sample.class).hit()).isFalse();
    }

    @Test
    void putWithJitterStaysWithinTenPercentOfConfiguredBaseTtl() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        for (int index = 0; index < 50; index++) {
            cacheService.putWithJitter(KEY, new Sample(index), 100);
        }

        ArgumentCaptor<Duration> durations = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations, org.mockito.Mockito.times(50))
                .set(org.mockito.ArgumentMatchers.eq(KEY),
                        org.mockito.ArgumentMatchers.anyString(), durations.capture());
        List<Long> values = durations.getAllValues().stream().map(Duration::toSeconds).toList();
        assertThat(values).allSatisfy(seconds -> assertThat(seconds).isBetween(100L, 110L));
    }

    private record Sample(int value) {
    }
}

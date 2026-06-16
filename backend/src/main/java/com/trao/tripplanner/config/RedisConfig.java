package com.trao.tripplanner.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// SOLID-SRP: Handles only Redis/cache infrastructure configuration
// Pattern-Singleton: All @Bean methods produce application-scoped singletons
@Configuration
@EnableCaching
public class RedisConfig {

    public static final String ITINERARY_CACHE = "itinerary_cache";
    public static final String HOTEL_CACHE = "hotel_cache";
    public static final String TRIP_CACHE = "trip_cache";

    // Private method — NOT a @Bean so it doesn't replace Spring Boot's primary ObjectMapper
    // (which would break Jackson HTTP message conversion for all REST endpoints)
    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Type info required so Redis can deserialize polymorphic cached objects
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        // AI-generated itineraries cached for 6 hours (expensive to regenerate)
        cacheConfigs.put(ITINERARY_CACHE, defaultConfig.entryTtl(Duration.ofHours(6)));
        // Hotel suggestions cached for 12 hours (relatively stable data)
        cacheConfigs.put(HOTEL_CACHE, defaultConfig.entryTtl(Duration.ofHours(12)));
        // Trip data cached for 10 minutes (frequently accessed list)
        cacheConfigs.put(TRIP_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}

package com.aurora.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 *
 * <p>显式定义 {@code RedisTemplate<String, Object>}，使用 String key + JSON value 序列化。
 * 避免默认 {@code RedisTemplate<Object, Object>}（JDK 序列化）造成的 Redis 内容乱码，
 * 同时提供 {@code RedisUtil} 所需的 {@code <String, Object>} 泛型类型——
 * 否则当 classpath 中存在 Redisson 时，其自动配置会创建 {@code RedisTemplate<Object, Object>}
 * 并抢占 {@code redisTemplate} bean，导致按泛型注入失败、应用启动报错。</p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}

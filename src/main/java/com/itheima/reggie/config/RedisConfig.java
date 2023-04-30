package com.itheima.reggie.config;

import com.itheima.reggie.utils.RedisBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    private static  final int NUM_APPROX_ELEMENTS = 3000;
    private static final double FPP = 0.03;
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory){
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        // 默认的Key序列化器为: JdkSerializationRedisSerializer
        // 直接存入redis的数据因为默认key序列化器的缘故，会造成在redis手动查询时与自己存储的不一致，因此改为StringRedisSerializer
        // 实际上并不需要改，因为很少有人去服务器自己查，现在只不过是在做实验，因此改了一下方便观察
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    @Bean
    public RedisBloomFilter getRedisBloomFilter() {
        RedisBloomFilter redisBloomFilter = new RedisBloomFilter();
        redisBloomFilter.init(NUM_APPROX_ELEMENTS, FPP);
        return redisBloomFilter;
    }

    @Bean
    public JedisPool getJedisPool(){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(8);
        jedisPoolConfig.setMaxIdle(8);
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, host, port);
        return jedisPool;
    }

}

package com.itheima.reggie;

import com.itheima.reggie.utils.RedisBloomFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestRedisBloomFilter {

    private static final int FIVE_MIN = 60 * 5;

    @Autowired
    private RedisBloomFilter redisBloomFilter;

    @Test
    public void tetsInsert(){
        System.out.println(redisBloomFilter);
        System.out.println("test RedisBloomFileter Insert Method");
        redisBloomFilter.insert("18834563456", "234522", FIVE_MIN);
        redisBloomFilter.insert("18865436543", "224522", FIVE_MIN);
        redisBloomFilter.insert("18812341234", "273982", FIVE_MIN);
    }

    @Test
    public void testMayExist(){
        System.out.println("test RedisBloomFileter testMayExist Method");
        System.out.println(redisBloomFilter.mayExist("18834563456", "234522"));
        System.out.println(redisBloomFilter.mayExist("18865436543", "224522"));
        System.out.println(redisBloomFilter.mayExist("18812341235", "234523"));
    }

}

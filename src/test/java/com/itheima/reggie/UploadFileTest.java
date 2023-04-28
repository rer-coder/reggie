package com.itheima.reggie;

import com.itheima.reggie.ReggieApplication;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.Set;

/**
 * @author hpc
 * @create 2023/3/10 16:31
 */
@SpringBootTest
public class UploadFileTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void test1() {
        String fileName = "mdkwdjek.jpg";
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        System.out.println(suffix);
    }

    @Test
    public void contextLoads(){
        Jedis jedis = new Jedis("10.18.1.214", 6379);
        System.out.println("运行成功");
        System.out.println(jedis.ping());
        jedis.connect();
        jedis.set("name", "DGJ");

        jedis.hset("stu", "name", "DGJ");
        jedis.hset("stu", "name", "HPC");

        Map<String, String> map = jedis.hgetAll("stu");
        Set<String> set = map.keySet();
        for(String key: set){
            String value = jedis.get(key);
            System.out.println(key + ":" + value);
        }
        jedis.close();
    }

    @Test
    void contextLoads2() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("gender", "male");
        String gender = (String) valueOperations.get("gender");
        System.out.println(gender);
    }
}

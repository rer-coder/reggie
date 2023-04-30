package com.itheima.reggie.utils;

import com.google.common.hash.Funnels;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.nio.charset.Charset;


/**
 * 仿照谷歌布隆过滤器做的布隆过滤器,区别在于谷歌的布隆过滤器基本上是本机使用的,
 * 该代码基于Redis的BitMap实现,可以使用于分布式生产环境中
 * 布隆过滤器所需hash函数和Bitmap的长度的计算公式为:
 *  BitMap的长度: m = -\frac{n \times \ln p}{(\ln 2)^{2}}
 *  Hash函数的个数: k = \ln 2 \times \frac{m}{n}
 */

public class RedisBloomFilter {

    public final static String RS_BF_NS = "rbf:"; // 用于组合Redis查询时key的前缀
    private int numApproxElements; // 预估元素的数量,需要手动指定
    private double fpp; // 可接受的最大误差,需要手动指定
    private int numHashFunctions; // 自动计算的hash函数的个数
    private int bitmapLength; // 自动计算的最大Bitmap的长度

    @Autowired
    private JedisPool jedisPool;

    /**
     * 布隆过滤器的构造
     * @param numApproxElements 预估元素的数量,需要手动指定
     * @param fpp 可接受的最大误差,需要手动指定
     * @retun 一个初始化后的BloomFilter对象
     */
    public RedisBloomFilter init(int numApproxElements, double fpp){
        this.numApproxElements = numApproxElements;
        this.fpp = fpp;

        this.bitmapLength = (int) ((-numApproxElements * Math.log(fpp)) / (Math.log(2) * Math.log(2)));
        this.numHashFunctions = Math.max(1, (int) -Math.round(Math.log(fpp) / Math.log(2)));

        return this;
    }

    /**
     * 计算一个元素值hash之后可以映射到Bimap的哪些bit上
     * 用两个Hash函数来模拟多个hash函数的情况
     * 将前64位的长整型作为第一个哈希值，后64位作为偏移量来计算后续的Hash值。
     * 具体来说，使用第二个长整型和第一个哈希值的和作为下一个哈希值，并将它们模除比特数组的长度，得到一个比特位置。
     * 这个过程重复多次，直到计算出指定数量的哈希值为止。
     */
    private long[] gitBitIndices(String element){
        long[] indices = new long[numHashFunctions];
        // MurmurHash3哈希函数具有高效性和良好的分布性，因此这种实现方式在实际应用中被广泛使用。
        byte[] bytes = Hashing.murmur3_128().
                hashObject(element, Funnels.stringFunnel(Charset.forName("utf-8"))).
                asBytes();
        // 拆分成两个64位的长整形
        long hash1 = Longs.fromBytes(bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
        long hash2 = Longs.fromBytes(bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8]);

        long combinedHash = hash1;
        for(int i= 0; i < numHashFunctions; i++){
            // 与Long.MAX_VALUE（0x7fffffffffffffffL）相&是为了保证得到的结果最高位永远是0，从而保证得到的结果是正数
            indices[i] = (combinedHash & Long.MAX_VALUE) % bitmapLength;
            combinedHash = combinedHash + hash2;
        }

        System.out.println(element + "数组下标");
        for (long index: indices) {
            System.out.println(index + "被选中");
        }
        return indices;
    }


    /**
     * 插入元素操作
     * @param key 未拼接前的key的值，会在操作完成key的拼接
     * @param element 元素的值，字符串类型
     * @param expireSec 过期时间（秒）
     */
    public void insert(String key, String element, int expireSec){
        if(key == null || element == null){
            throw  new RuntimeException("键值对不能为空");
        }
        String realKey = RS_BF_NS.concat(key);
        try(Jedis jedis = jedisPool.getResource()){
            try(Pipeline pipeline = jedis.pipelined()){
                for(long index: gitBitIndices(element)){
                    pipeline.setbit(realKey, index, true);
                }
                pipeline.syncAndReturnAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
            jedis.expire(realKey, expireSec);
        }
    }

    /**
     * 检查元素是否可能存在
     * @param key 原始的Redis的key，要加上前缀
     * @param element 元素值，字符串类型
     * @return
     */
    public boolean mayExist(String key, String element){
        if(key == null || element == null){
            throw new RuntimeException("键值不能为空");
        }
        String realKey = RS_BF_NS.concat(key);
        boolean result = false;

        try(Jedis jedis = jedisPool.getResource()){
            try(Pipeline pipeline = jedis.pipelined()){
                for(long index : gitBitIndices(element)){
                    pipeline.getbit(realKey, index);
                }
                result = !pipeline.syncAndReturnAll().contains(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "RedisBloomFilter{" +
                "numApproxElements=" + numApproxElements +
                ", fpp=" + fpp +
                ", numHashFunctions=" + numHashFunctions +
                ", bitmapLength=" + bitmapLength +
                ", jedisPool=" + jedisPool +
                '}';
    }
}

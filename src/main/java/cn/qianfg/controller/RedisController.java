package cn.qianfg.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/redis")
public class RedisController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisController.class);

    @Autowired
    private RedisTemplate redisTemplate = null;

    @Autowired
    private StringRedisTemplate stringRedisTemplate = null;

    /**
     * 操作String和Hash
     */
    @RequestMapping("/stringAndHash")
    @ResponseBody
    public Map<String, Object> testStringAndHash() {
        redisTemplate.opsForValue().set("key1", "value1");
        //注意这里使用了 JDK 的序列化器 ,所以 Redis 保存时不是整数, 不能运算
        redisTemplate.opsForValue().set("int_key", "1");
        stringRedisTemplate.opsForValue().set("int", "1");
        //使用运算
        stringRedisTemplate.opsForValue().increment("int", 1);

        Map<String, Object> hash = new HashMap<>();
        hash.put("field1", "value1");
        hash.put("field2", "value2");
        //将Hashmap存储到redis中
        stringRedisTemplate.opsForHash().putAll("hash2", hash);
        stringRedisTemplate.opsForHash().put("hash2", "field3", "value3");

        //绑定散列操作的 key,这样可以连续对同一个散列数据类型进行操作
        BoundHashOperations hashOps = stringRedisTemplate.boundHashOps("hash2");
        //删除元素
        hashOps.delete("field2", "field1");
        //添加元素
        hashOps.put("field4", "value4");
        LOGGER.info(hashOps.entries().toString());
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        return map;
    }

    /**
     * 操作List
     */
    @RequestMapping("/list")
    @ResponseBody
    public Map<String, Object> testList() {
        //链表从左到右的顺序为v10, v8, v6, v4, v2
        stringRedisTemplate.opsForList().leftPushAll("list1", "v2", "v4", "v6", "v8", "v10");
        //链表从左到右的顺序为v1, v3, v5, v7, v9
        stringRedisTemplate.opsForList().rightPushAll("list2", "v1", "v3", "v5", "v7", "v9");
        //绑定list2操作链表
        BoundListOperations listOps = stringRedisTemplate.boundListOps("list2");
        //从右边弹出一个成员
        Object result1 = listOps.rightPop();
        LOGGER.info("list2的最右边元素为: " + result1.toString());
        //获取定位元素, 下标从0开始
        Object result2 = listOps.index(1);
        LOGGER.info("list2下标为1的元素为" + result2.toString());
        //从左边插入链表
        listOps.leftPush("v0");
        //求链表长
        Long size = listOps.size();
        LOGGER.info("list2的长度为: " + size);
        //求链表区间成员
        List element = listOps.range(0, size - 2);
        LOGGER.info("list2从0到size-2的元素依次为: " + element.toString());

        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        return map;
    }

    /**
     * 操作set
     */
    @RequestMapping("/set")
    @ResponseBody
    public Map<String, Object> testSet() {
        //重复的元素不会被插入
        stringRedisTemplate.opsForSet().add("set1", "v1", "v1", "v3", "v5", "v7", "v9");
        stringRedisTemplate.opsForSet().add("set2", "v2", "v4", "v6", "v5", "v10", "v10");
        //绑定sert1集合操作
        BoundSetOperations setOps = stringRedisTemplate.boundSetOps("set1");
        setOps.add("v11", "v13");
        setOps.remove("v1", "v3");
        //返回所有元素
        Set set = setOps.members();
        LOGGER.info("集合中所有元素: " + set.toString());
        //求成员数
        Long size = setOps.size();
        LOGGER.info("集合长度: " + String.valueOf(size));
        //求交集
        Set inner = setOps.intersect("set2");
        //求交集并用新的集合保存
        setOps.intersectAndStore("set2", "set1_set2");
        LOGGER.info("集合的交集: " + inner.toString());
        //求差集
        Set diff = setOps.diff("set2");
        //求差集并用新的集合保存
        setOps.diffAndStore("set2", "set1-set2");
        LOGGER.info("集合的差集: " + diff.toString());
        //求并集
        Set union = setOps.union("set2");
        //求并集并用新的集合保存
        setOps.unionAndStore("set2", "set1=set2");
        LOGGER.info("集合的并集: " + union.toString());
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        return map;
    }

    /**
     * redis操作有序集合
     */
    @RequestMapping("/zset")
    @ResponseBody
    public Map<String, Object> testZSet() {
        Set<ZSetOperations.TypedTuple<String>> typedTupleSet = new HashSet<>();
        for (int i = 1; i <= 9; i++) {
            //分数
            double score = i * 0.1;
            //创建一个TypedTuple对象, 存入值和分数
            ZSetOperations.TypedTuple typedTuple = new DefaultTypedTuple<String>("value" + i, score);
            typedTupleSet.add(typedTuple);
        }
        LOGGER.info("新建的set: " + typedTupleSet.toString());
        //往有序集合插入元素
        stringRedisTemplate.opsForZSet().add("zset1", typedTupleSet);
        //绑定zset1有序集合操作
        BoundZSetOperations<String, String> zSetOps = stringRedisTemplate.boundZSetOps("zset1");
        zSetOps.add("value10", 0.26);
        Set<String> setRange = zSetOps.range(1, 6);
        LOGGER.info("下标下1-6的set: " + setRange.toString());

        //按分数排序获取有序集合
        Set<String> setScore = zSetOps.rangeByScore(0.2, 0.6);
        LOGGER.info("按分数排序获取有序集合: " + setScore.toString());
        //定义值范围
        RedisZSetCommands.Range range = new RedisZSetCommands.Range();
        //大于value3
        range.gt("value3");
        //小于等于value8
        range.lte("value8");

        //按值排序, 注意这个排序是按字符串排序
        Set<String> setLex = zSetOps.rangeByLex(range);
        LOGGER.info("按值排序: " + setLex.toString());

        //删除元素
        zSetOps.remove("value9", "value2");
        //求分数
        Double score = zSetOps.score("value8");
        LOGGER.info("求value8的分数: " + score);

        //在下标区间 按分数排序, 同时返回value和score
        Set<ZSetOperations.TypedTuple<String>> rangeSet = zSetOps.rangeWithScores(1, 6);
        LOGGER.info("在下标区间 按分数排序, 同时返回value和score:  " + rangeSet.toString());

        //在下标区间 按分数排序, 同时返回value和score
        Set<ZSetOperations.TypedTuple<String>> scoreSet = zSetOps.rangeByScoreWithScores(1, 6);
        LOGGER.info("在下标区间 按分数排序, 同时返回value和score:  " + scoreSet.toString());

        //按从大到小排序
        Set<String> reverseSet = zSetOps.reverseRange(2, 8);
        LOGGER.info("按从大到小排序: " + reverseSet.toString());

        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        return map;
    }
}
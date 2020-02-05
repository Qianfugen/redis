package cn.qianfg.config;

import cn.qianfg.pojo.User;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
class MyConfigRedisTemplateTest {

    //在MyRedisConfig文件中配置了redisTemplate的序列化之后， 客户端也能正确显示键值对了
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void test() {
        redisTemplate.opsForValue().set("name", "qianfg");
        System.out.println(redisTemplate.opsForValue().get("name"));
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setId(i);
            user.setName(String.format("测试%d", i));
            user.setAge(i + 10);
            map.put(String.valueOf(i), user);
        }
        redisTemplate.opsForHash().putAll("测试", map);
        BoundHashOperations hashOps = redisTemplate.boundHashOps("测试");
        Map map1 = hashOps.entries();
        System.out.println(map1);
    }
}
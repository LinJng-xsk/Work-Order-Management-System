package com.campus.woms;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.Resource;

@RestController
public class RedisTestController {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @GetMapping("/testRedis")
    public String test(){
        redisTemplate.opsForValue().set("testKey","redis连接成功✅");
        String val = (String) redisTemplate.opsForValue().get("testKey");
        return val;
    }
}

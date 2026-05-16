package com.spzx.product.service.impl;

import com.spzx.common.core.utils.StringUtils;
import com.spzx.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    StringRedisTemplate stringRedisTemplate; //SpringBoot自动化配置
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public synchronized void testLock() {
        // 第一版：尝试获取锁（当上完锁后如果出现了异常，会导致锁无法释放，可以设置过期时间解决）
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent("lock", "hello");
        // 第二版：设置key的同时设置过期时间（如果业务的执行时间大于锁的过期时间，那个会出现释放别人锁的情况）
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent("lock", "hello",3, TimeUnit.SECONDS);
        // 第三版：设置key的同时，随机生成一个value（使用UUID随机生成），释放时判断是否是自己的key
        String lockValue = UUID.randomUUID().toString().replace("-", "");
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent("lock", lockValue,3, TimeUnit.SECONDS);
        // 第四版：是使用Lua脚本保证判断和删除锁具有原子性

        if (flag) {
            // 如果拿到了锁，就执行相应的业务
            try {
                // 查询Redis中的num值
                String value = (String) this.stringRedisTemplate.opsForValue().get("num");
                // 没有该值return
                if (StringUtils.isBlank(value)) {
                    return;
                }
                // 有值就转成成int
                int num = Integer.parseInt(value);
                // 把Redis中的num值+1
                this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num)); //  java 中  ++ 操作不是原子的。
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            } finally {
                /*// 获取Redis中的值
                String redisLockValue = stringRedisTemplate.opsForValue().get("lock");
                // 判断是否是自己的锁
                if(redisLockValue.equals(lockValue)){
                    // 释放锁
                    stringRedisTemplate.delete("lock");
                }*/
                String LuaScript = """
                        if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end
                        """;
                RedisScript<Boolean> redisScript = RedisScript.of(LuaScript, Boolean.class);
                List<String> list = Arrays.asList("lock"); // 通过key来获取value
                stringRedisTemplate.execute(redisScript, list, lockValue);
            }
        }else{
            // 如果没有拿到锁，就等待，然后再次获取锁
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

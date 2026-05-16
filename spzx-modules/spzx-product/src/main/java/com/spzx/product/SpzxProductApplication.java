package com.spzx.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spzx.common.security.annotation.EnableCustomConfig;
import com.spzx.common.security.annotation.EnableRyFeignClients;
import com.spzx.product.api.domain.Product;
import com.spzx.product.api.domain.ProductSku;
import com.spzx.product.mapper.ProductMapper;
import com.spzx.product.mapper.ProductSkuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;


@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
@Slf4j
public class SpzxProductApplication implements CommandLineRunner {
    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    public static void main(String[] args) {
        SpringApplication.run(SpzxProductApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }

    @Override
    public void run(String... args) throws Exception {
        // 设置redis bit，Redis位图，当启动程序时，将商品的sku放到位图当中，当查询时判断sku是否存在
        // 先从数据库中查询productSkus表，然后再通过stream()来获取id
        List<ProductSku> productSkus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getStatus,1));
        // 将sku放到Redis的位图当中
        String key = "product:sku:data";
        productSkus.forEach(sku -> {
            redisTemplate.opsForValue().setBit(key,sku.getId(),true);
            log.info("将skuId放到Redis的位图中：" + key + sku.getId());
        });

    }
}

package com.spzx.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spzx.common.core.exception.ServiceException;
import com.spzx.common.core.utils.bean.BeanUtils;
import com.spzx.common.core.web.domain.BaseEntity;
import com.spzx.product.api.domain.Product;
import com.spzx.product.api.domain.ProductDetails;
import com.spzx.product.api.domain.ProductSku;
import com.spzx.product.api.domain.vo.SkuPrice;
import com.spzx.product.api.domain.vo.SkuQuery;
import com.spzx.product.api.domain.vo.SkuStockVo;
import com.spzx.product.domain.SkuStock;
import com.spzx.product.mapper.ProductDetailsMapper;
import com.spzx.product.mapper.ProductMapper;
import com.spzx.product.mapper.ProductSkuMapper;
import com.spzx.product.mapper.SkuStockMapper;
import com.spzx.product.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.spi.LocaleNameProvider;
import java.util.stream.Collectors;

/**
 * 商品Service业务层处理
 */
@Slf4j
@Service
@Transactional
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private ProductDetailsMapper productDetailsMapper;

    @Autowired
    private SkuStockMapper skuStockMapper;

    //@Autowired
    //private StringRedisTemplate stringRedisTemplate; //适合 key和value都是字符串类型

    @Autowired
    private RedisTemplate redisTemplate; //适合值是任意类型

    /**
     * 查询商品列表
     *
     * @param product 商品
     * @return 商品
     */
    @Override
    public List<Product> selectProductList(Product product) {
        return productMapper.selectProductList(product);
    }

    //原子性
    @Override
    public int insertProduct(Product product) {
        //1.保存Product对象到product表
        productMapper.insert(product); //主键回填

        //2.保存List<ProductSku>对象到product_sku表
        List<ProductSku> productSkuList = product.getProductSkuList();
        if (CollectionUtils.isEmpty(productSkuList)) {
            throw new ServiceException("SKU数据为空");
        }
        int size = productSkuList.size();
        for (int i = 0; i < size; i++) {
            ProductSku productSku = productSkuList.get(i);
            productSku.setSkuCode(product.getId() + "_" + i);
            productSku.setSkuName(product.getName() + " " + productSku.getSkuSpec());
            productSku.setProductId(product.getId());
            productSkuMapper.insert(productSku);

            //添加商品库存  //3.保存List<SkuStock>对象到sku_stock表
            SkuStock skuStock = new SkuStock();
            skuStock.setSkuId(productSku.getId());
            skuStock.setTotalNum(productSku.getStockNum());
            skuStock.setLockNum(0);
            skuStock.setAvailableNum(productSku.getStockNum());
            skuStock.setSaleNum(0);
            skuStockMapper.insert(skuStock);
        }

        //4.保存ProductDetails对象到product_details表
        ProductDetails productDetails = new ProductDetails();
        productDetails.setImageUrls(String.join(",", product.getDetailsImageUrlList()));
        productDetails.setProductId(product.getId());
        productDetailsMapper.insert(productDetails);

        return 1;
    }


    @Override
    public Product selectProductById(Long id) {
        //1.根据id查询Product对象
        Product product = productMapper.selectById(id);

        //2.封装扩展字段：查询商品对应多个List<ProductSku>
        //select * from product_sku where product_id =?
        List<ProductSku> productSkuList = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        List<Long> productSkuIdList = productSkuList.stream().map(productSku -> productSku.getId()).toList();


        // select * from sku_stock where sku_id in (1,2,3,4,5,6)
        List<SkuStock> skuStockList = skuStockMapper.selectList(new LambdaQueryWrapper<SkuStock>().in(SkuStock::getSkuId, productSkuIdList));

        Map<Long, Integer> skuIdToTatalNumMap = skuStockList.stream().collect(Collectors.toMap(SkuStock::getSkuId, SkuStock::getTotalNum));
        productSkuList.forEach(productSku -> {
            //返回ProductSku对象，携带了库存数据；
            productSku.setStockNum(skuIdToTatalNumMap.get(productSku.getId()));
        });

        product.setProductSkuList(productSkuList);

        //3.封装扩展字段：商品详情图片List<String>
        ProductDetails productDetails = productDetailsMapper.selectOne(new LambdaQueryWrapper<ProductDetails>().eq(ProductDetails::getProductId, id));
        String imageUrls = productDetails.getImageUrls();   //url,url,url
        String[] urls = imageUrls.split(",");
        product.setDetailsImageUrlList(Arrays.asList(urls));
        //返回Product对象
        return product;
    }


    @Override
    public int updateProduct(Product product) {
        //2.更新SKU   List<ProductSku>（使用双删除的模式删除数据）
        // 在更新之前删除缓存,根据SkuId来进行删除Sku的缓存数据
        List<ProductSku> productSkuList = product.getProductSkuList();
        List<Long> skuIds = productSkuList.stream().map(BaseEntity::getId).collect(Collectors.toList());
        skuIds.forEach(skuId -> {
            redisTemplate.delete("product:sku:" + skuId);
            log.info("更新是调用了删除方法" + skuId);
        });

        //1.更新Product
        productMapper.updateById(product);


        // 更新SKU
        if (CollectionUtils.isEmpty(productSkuList)) {
            throw new ServiceException("SKU数据为空");
        }
        productSkuList.forEach(productSku -> {
            productSkuMapper.updateById(productSku);

            //3.更新库存   List<ProductSku> -> 获取扩展字段stockNum
            SkuStock skuStock = skuStockMapper.selectOne(new LambdaQueryWrapper<SkuStock>().eq(SkuStock::getSkuId, productSku.getId()));
            skuStock.setTotalNum(productSku.getStockNum());
            skuStock.setAvailableNum(skuStock.getTotalNum() - skuStock.getLockNum());
            skuStockMapper.updateById(skuStock);
        });

        //4.更新详情ProductDetails
        ProductDetails productDetails = productDetailsMapper
                .selectOne(new LambdaQueryWrapper<ProductDetails>().eq(ProductDetails::getProductId, product.getId()));
        productDetails.setImageUrls(String.join(",", product.getDetailsImageUrlList()));
        productDetailsMapper.updateById(productDetails);

        // 睡觉延迟
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 再次进行删除
        skuIds.forEach(skuId -> {
            redisTemplate.delete("product:sku:" + skuId);
        });

        return 1;
    }


    @Override
    public int deleteProductByIds(Long[] ids) {
        //1.删除Product表数据
        // delete from product where id in (1,2)
        productMapper.deleteBatchIds(Arrays.asList(ids));

        //2.删除ProductSku表数据
        List<ProductSku> productSkuList = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>().in(ProductSku::getProductId, Arrays.asList(ids)));
        List<Long> productSkuIdList = productSkuList.stream().map(ProductSku::getId).toList();
        productSkuMapper.deleteBatchIds(productSkuIdList);

        //3.删除SkuStock表数据
        skuStockMapper.delete(new LambdaQueryWrapper<SkuStock>().in(SkuStock::getSkuId, productSkuIdList));

        //4.删除ProductDetails表数据
        // delete from product_details where product_id in (1,2)
        productDetailsMapper.delete(new LambdaQueryWrapper<ProductDetails>().in(ProductDetails::getProductId, Arrays.asList(ids)));
        return 1;
    }


    @Override
    public void updateAuditStatus(Long id, Integer auditStatus) {
        Product product = new Product();
        product.setId(id);
        if (auditStatus == 1) {
            product.setAuditStatus(1);
            product.setAuditMessage("审批通过");
        } else {
            product.setAuditStatus(-1);
            product.setAuditMessage("审批拒绝");
        }
        productMapper.updateById(product);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = new Product();
        product.setId(id);
        // 更新状态时，同时更新Redis中的位图
        List<ProductSku> productSkus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        // 通过productSkus获取skuIds
        List<Long> skuIds = productSkus.stream().map(ProductSku::getId).toList();

        if (status == 1) {
            product.setStatus(1);
            // 放入到位图当中
            skuIds.forEach(skuId -> {
                redisTemplate.opsForValue().setBit("product:sku:data", skuId, true);
            });
        } else {
            product.setStatus(-1);
            // 从位图中删除
            skuIds.forEach(skuId -> {
                redisTemplate.opsForValue().setBit("product:sku:data", skuId, false);
            });
        }
        productMapper.updateById(product);

        // 更新对应商品的sku状态
        productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSku>().set(ProductSku::getStatus, status).eq(ProductSku::getProductId, id));
    }


    @Override
    public List<ProductSku> getTopSale() {
        return productSkuMapper.getTopSale();
    }


    @Override
    public List<ProductSku> skuList(SkuQuery skuQuery) {
        return productSkuMapper.skuList(skuQuery);
    }


    /**
     * 服务提供者：6个接口来服务于商品详情查询。需要进行优化，提供查询效率。
     * 需要使用redis来提高性能。
     */

    @Override
    public ProductSku getProductSku(Long skuId) {
        try {
            // 查询缓存，如果缓存中存在，直接返回
            //1.1 构建业务数据Key 形式：前缀+业务唯一标识
            String dataKey = "product:sku:" + skuId;
            // 1.2 从Redis中查询业务数据
            ProductSku productSku = (ProductSku) redisTemplate.opsForValue().get(dataKey);
//        if(null != productSku) {
            if (redisTemplate.hasKey(dataKey)) {
                // 如果缓存中存在业务数据，直接返回，不必从数据库中查询
                log.info("命中缓存，直接返回，线程ID：{}，线程名称：{}", Thread.currentThread().getId(), Thread.currentThread().getName());
                return productSku;
            }
            // 如果在缓存中没有查到数据，直接从数据库中查询数据，然后放到缓存当中
            // 从数据库中查询数据，需要上分布式锁
            String lockVal = UUID.randomUUID().toString().replace("-", "");
            String lockKey = "product:lock" + skuId;
            // 尝试获取锁
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockKey, lockVal, 3, TimeUnit.SECONDS);
            if (flag) {
                try {
                    // 获取到锁，查询数据，放到缓存当中
                    log.info("从数据库中查询数据");
                    ProductSku productSkuBySkuId = getProductSkuBySkuId(skuId);
                    redisTemplate.opsForValue().set(dataKey, productSkuBySkuId);
                    return productSkuBySkuId;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 使用Lua脚本查询和删除锁，避免删除了别人的锁
                    String LuaScript = """
                            if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end
                            """;
                    RedisScript<Boolean> redisScript = RedisScript.of(LuaScript, Boolean.class);
                    List<String> list = Arrays.asList(lockKey); // 通过key来获取value
                    redisTemplate.execute(redisScript, list, lockVal);
                }
            } else { // 自旋
                // 如果没有获取到锁，就睡觉等待锁(
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return getProductSku(skuId);
            }
        } catch (Exception e) {
            //兜底处理方案：Redis服务有问题，将业务数据获取自动从数据库获取
            log.error("[商品服务]查询商品信息异常：{}", e);
            return getProductSkuBySkuId(skuId);
        }
    }

    public ProductSku getProductSkuBySkuId(Long skuId) {
        return productSkuMapper.selectById(skuId);
    }


    @Override
    public Product getProduct(Long id) {
        return productMapper.selectById(id);
    }


    @Override
    public SkuPrice getSkuPrice(Long skuId) {
        ProductSku productSku = productSkuMapper.selectOne(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getId, skuId).select(ProductSku::getSalePrice, ProductSku::getMarketPrice));
        SkuPrice skuPrice = new SkuPrice();
        BeanUtils.copyProperties(productSku, skuPrice);
        return skuPrice;
    }


    @Override
    public ProductDetails getProductDetails(Long id) {
        return productDetailsMapper.selectOne(new LambdaQueryWrapper<ProductDetails>().eq(ProductDetails::getProductId, id));
    }


    @Override
    public Map<String, Long> getSkuSpecValue(Long id) {
        List<ProductSku> productSkuList = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id).select(ProductSku::getId, ProductSku::getSkuSpec));
        Map<String, Long> skuSpecValueMap = new HashMap<>();
        productSkuList.forEach(item -> {
            skuSpecValueMap.put(item.getSkuSpec(), item.getId());
        });
        return skuSpecValueMap;
    }


    @Override
    public SkuStockVo getSkuStock(Long skuId) {
        SkuStock skuStock = skuStockMapper.selectOne(new LambdaQueryWrapper<SkuStock>().eq(SkuStock::getSkuId, skuId));
        SkuStockVo skuStockVo = new SkuStockVo();
        BeanUtils.copyProperties(skuStock, skuStockVo);
        return skuStockVo;
    }


    // select * from product_sku where id in (1,2,3)
    // select id,sale_price,market_price from product_sku where id in (1,2,3)
    @Override
    public List<SkuPrice> getSkuPriceList(List<Long> skuIdList) {
        if (CollectionUtils.isEmpty(skuIdList)) {
            return new ArrayList<SkuPrice>();
        }
        List<ProductSku> skuList = productSkuMapper
                .selectList(new LambdaQueryWrapper<ProductSku>().in(ProductSku::getId, skuIdList)
                        .select(ProductSku::getId, ProductSku::getSalePrice, ProductSku::getMarketPrice));
        if (CollectionUtils.isEmpty(skuList)) {
            return new ArrayList<SkuPrice>();
        }
        return skuList.stream().map((sku) -> {
            SkuPrice skuPrice = new SkuPrice();
            skuPrice.setSkuId(sku.getId());
            skuPrice.setSalePrice(sku.getSalePrice());
            skuPrice.setMarketPrice(sku.getMarketPrice());
            return skuPrice;
        }).toList();
    }

    @Override
    public String checkAndSubStock(Long skuId, Integer skuNum) {
        int count = skuStockMapper.checkAndSubStock(skuId, skuNum);
        if (count == 0) { // 更新语句没有执行,库存不足
            // 查询商品名称
            ProductSku productSku = productSkuMapper.selectOne(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getId, skuId));
            // 查询商品的库存
            SkuStock skuStock = skuStockMapper.selectOne(new LambdaQueryWrapper<SkuStock>().eq(SkuStock::getSkuId, skuId));
            return productSku.getSkuName() + "库存不足,库存只有" + skuStock.getAvailableNum() + "个,您购买你的数量是" + skuNum + "个";
        }
        return "";
    }
}
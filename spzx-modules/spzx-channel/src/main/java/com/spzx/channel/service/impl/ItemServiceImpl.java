package com.spzx.channel.service.impl;

import com.alibaba.fastjson2.JSON;
import com.spzx.channel.domain.ItemVo;
import com.spzx.channel.service.IItemService;
import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.exception.ServiceException;
import com.spzx.product.api.RemoteProductService;
import com.spzx.product.api.domain.Product;
import com.spzx.product.api.domain.ProductDetails;
import com.spzx.product.api.domain.ProductSku;
import com.spzx.product.api.domain.vo.SkuPrice;
import com.spzx.product.api.domain.vo.SkuStockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
@Slf4j
public class ItemServiceImpl implements IItemService {

    @Autowired
    private RemoteProductService remoteProductService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public ItemVo item(Long skuId) throws Exception {

        // 首先判断位图是否存在，如果不存在，直接抛出异常，不用查询是数据库了
        Boolean flag = redisTemplate.opsForValue().getBit("product:sku:data", skuId);
        if(!flag){
            // 商品Sku不存在，抛出异常
            throw new ServiceException("您访问的skuI为"+skuId+"商品不存在");
        }

        ItemVo itemVo = new ItemVo();

        //任务1.获取sku信息
        R<ProductSku> productSkuResult = remoteProductService.getProductSku(skuId, SecurityConstants.INNER);
        if (R.FAIL == productSkuResult.getCode()) {
            throw new ServiceException(productSkuResult.getMsg());
        }
        ProductSku productSku = productSkuResult.getData();
        itemVo.setProductSku(productSku);


        //任务2.获取商品信息
        R<Product> productResult = remoteProductService.getProduct(productSku.getProductId(), SecurityConstants.INNER);
        if (R.FAIL == productResult.getCode()) {
            throw new ServiceException(productResult.getMsg());
        }
        Product product = productResult.getData();
        itemVo.setProduct(product);
        itemVo.setSliderUrlList(Arrays.asList(product.getSliderUrls().split(",")));
        itemVo.setSpecValueList(JSON.parseArray(product.getSpecValue()));


        //任务3.获取商品最新价格
        R<SkuPrice> skuPriceResult = remoteProductService.getSkuPrice(skuId, SecurityConstants.INNER);
        if (R.FAIL == skuPriceResult.getCode()) {
            throw new ServiceException(skuPriceResult.getMsg());
        }
        SkuPrice skuPrice = skuPriceResult.getData();
        itemVo.setSkuPrice(skuPrice);


        //任务4.获取商品详情
        R<ProductDetails> productDetailsResult = remoteProductService.getProductDetails(productSku.getProductId(), SecurityConstants.INNER);
        if (R.FAIL == productDetailsResult.getCode()) {
            throw new ServiceException(productDetailsResult.getMsg());
        }
        ProductDetails productDetails = productDetailsResult.getData();
        itemVo.setDetailsImageUrlList(Arrays.asList(productDetails.getImageUrls().split(",")));


        //任务5.获取商品规格对应商品skuId信息
        R<Map<String, Long>> skuSpecValueResult = remoteProductService.getSkuSpecValue(productSku.getProductId(), SecurityConstants.INNER);
        if (R.FAIL == skuSpecValueResult.getCode()) {
            throw new ServiceException(skuSpecValueResult.getMsg());
        }
        Map<String, Long> skuSpecValueMap = skuSpecValueResult.getData();
        itemVo.setSkuSpecValueMap(skuSpecValueMap);


        //任务6.获取商品库存信息
        R<SkuStockVo> skuStockResult = remoteProductService.getSkuStock(skuId, SecurityConstants.INNER);
        if (R.FAIL == skuStockResult.getCode()) {
            throw new ServiceException(skuStockResult.getMsg());
        }
        SkuStockVo skuStockVo = skuStockResult.getData();
        itemVo.setSkuStockVo(skuStockVo);
        productSku.setStockNum(skuStockVo.getAvailableNum());

        return itemVo;
    }
}
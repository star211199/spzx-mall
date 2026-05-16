package com.spzx.cart.service.impl;

import com.spzx.cart.api.domain.CartInfo;
import com.spzx.cart.service.ICartService;
import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.context.SecurityContextHolder;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.exception.ServiceException;
import com.spzx.product.api.RemoteProductService;
import com.spzx.product.api.domain.ProductSku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class CartServiceImpl implements ICartService {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    RemoteProductService remoteProductService;

    // 获取Redis中购物车中的key的方法
    public String getCartKey() {
        // 使用字符串拼接cart在购物车中的key,为每个用户分配购物车
        Long userId = SecurityContextHolder.getUserId();
        return "user:cart:" + userId;
    }

    @Override
    public void add2Cart(Long skuId, Integer num) {
        // 获取用户购物车key，用户的商品用hashmap存储在Redis中，filed为商品的skuId
        String cartKey = getCartKey();
        // 查询Redis中是否有用户的购物车
        CartInfo cartInfo = (CartInfo)redisTemplate.opsForHash().get(cartKey, skuId.toString());
        // 判断购物车商品是否存在
        if (null != cartInfo) {
            // 如果购物车商品存在，只需要加上数量即可
            cartInfo.setSkuNum(Math.min((cartInfo.getSkuNum() + num),99));
        }else{ // 第一次初始化商品购物车
            // 第一次将商品添加到购物车当中
            cartInfo = new CartInfo();
            // 设置商品的数量为1，当初次将商品加入购物车时，数量为1
            cartInfo.setSkuNum(1);
            cartInfo.setSkuId(skuId);
            // 将商品添加到购物车的时间
            cartInfo.setCreateTime(new Date());
            // 设置添加购物车用户的名字
            cartInfo.setCreateBy(SecurityContextHolder.getUserName());
            // 远程调用商品的微服务根据商品SkuId查询商品的sku信息
            R<ProductSku> productSku = remoteProductService.getProductSku(skuId, SecurityConstants.INNER);
            // 判断是否出现异常
            if(R.FAIL == productSku.getCode()){
                // 远程调用失败
                throw new ServiceException(productSku.getMsg());
            }
            // 获取商品的sku信息
            ProductSku productSku1 = productSku.getData();
            // 给购物项设置商品的sku信息
            cartInfo.setSkuName(productSku1.getSkuName());
            cartInfo.setThumbImg(productSku1.getThumbImg());
            // 放入购物车的价格
            cartInfo.setCartPrice(productSku1.getSalePrice());
            // 设置商品的实时价格
            cartInfo.setSkuPrice(productSku1.getSalePrice());
        }
        // 将购物车放到Redis中
        redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfo);
    }

    @Override
    public List<CartInfo> cartList() {
        // 获取用户购物车key
        String cartKey = getCartKey();
        // 获取Redis中hashmap中的所有value
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        // 遍历所有的购物项,更新实时价格
        cartInfoList.forEach(cartInfo -> {
            // 远程调用查询productSku
            R<ProductSku> productSkuR = remoteProductService.getProductSku(cartInfo.getSkuId(), SecurityConstants.INNER);
            // 判断是否出现异常
            if(R.FAIL == productSkuR.getCode()){
                // 远程调用失败
                throw new ServiceException(productSkuR.getMsg());
            }
            ProductSku productSku = productSkuR.getData();
            cartInfo.setSkuPrice(productSku.getSalePrice());
            // 将更新价格后的商品放入到Redis当中
            redisTemplate.opsForHash().put(getCartKey(), cartInfo.getSkuId().toString(), cartInfo);
        });
        return cartInfoList;
    }

    @Override
    public void updateCartInfoStatus(Long skuId, Integer status) {
        // 获取用户商品购物车
        String cartKey = getCartKey();
        CartInfo cartInfo = (CartInfo)redisTemplate.opsForHash().get(cartKey, skuId.toString());
        cartInfo.setIsChecked(status);
        // 放到Redis中更新
        redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfo);
    }

    @Override
    public void updateAllCartInfoStatus(Integer status) {
        // 获取购物车中所有的商品
        String cartKey = getCartKey();
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        cartInfoList.forEach(cartInfo -> {
            cartInfo.setIsChecked(status);
            redisTemplate.opsForHash().put(getCartKey(), cartInfo.getSkuId().toString(), cartInfo);
        });
    }

    @Override
    public void deleteCart(Long skuId) {
        // 在Redis中删除filed为skuId的商品
        String cartKey = getCartKey();
        redisTemplate.opsForHash().delete(cartKey, skuId.toString());
    }

    @Override
    public void clearCart() {
        // 在Redis中删除所有的购物车商品,删除这个key
        String cartKey = getCartKey();
        redisTemplate.delete(cartKey);
    }

    @Override
    public List<CartInfo> getCheckedCartInfo() {
        // 获取购物车中所有的商品
        String cartKey = getCartKey();
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        List<CartInfo> list = cartInfoList.stream().filter(cartInfo -> cartInfo.getIsChecked() == 1).toList();
        return list;
    }

    @Override
    public void clearCheckedCartInfo() {
        // 获取购物车的key
        String cartKey = getCartKey();
        // 获取购物车中选中的方法
        List<CartInfo> checkedCartInfo = getCheckedCartInfo();
        // 从Redis中删除
        checkedCartInfo.forEach(cartInfo -> {
            redisTemplate.opsForHash().delete(cartKey, cartInfo.getSkuId().toString());
        });
    }
}
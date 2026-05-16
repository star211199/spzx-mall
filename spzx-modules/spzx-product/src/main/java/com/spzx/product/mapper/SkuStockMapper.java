package com.spzx.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spzx.product.domain.SkuStock;
import org.apache.ibatis.annotations.Param;

/**
 * 商品skuMapper接口
 */
public interface SkuStockMapper extends BaseMapper<SkuStock> {
    //检查库存
    SkuStock check(@Param("skuId") Long skuId,@Param("skuNum")  Integer skuNum);

    //锁定库存
    int lock(@Param("skuId") Long skuId,@Param("skuNum")  Integer skuNum);

    int unlock(@Param("skuId") Long skuId,@Param("skuNum")  Integer skuNum);

    int minus(@Param("skuId") Long skuId,@Param("skuNum")  Integer skuNum);

    int checkAndSubStock(@Param("skuId") Long skuId,@Param("skuNum") Integer skuNum);
}
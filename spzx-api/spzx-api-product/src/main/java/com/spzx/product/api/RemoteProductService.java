package com.spzx.product.api;

import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.constant.ServiceNameConstants;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.web.page.TableDataInfo;
import com.spzx.product.api.domain.Product;
import com.spzx.product.api.domain.ProductDetails;
import com.spzx.product.api.domain.ProductSku;
import com.spzx.product.api.domain.vo.SkuLockVo;
import com.spzx.product.api.domain.vo.SkuPrice;
import com.spzx.product.api.domain.vo.SkuQuery;
import com.spzx.product.api.domain.vo.SkuStockVo;
import com.spzx.product.api.factory.RemoteProductFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "remoteProductService",
        value = ServiceNameConstants.PRODUCT_SERVICE,
        fallbackFactory = RemoteProductFallbackFactory.class)
public interface RemoteProductService {

    @GetMapping("/product/getTopSale")
    public R<List<ProductSku>> getTopSale(@RequestHeader(SecurityConstants.FROM_SOURCE) String resource);

    /**
     * @SpringQueryMap SkuQuery skuQuery
     * 由于是get请求，没有请求体，所以需要用@SpringQueryMap注解声明，表示将bean属性名称和值，以请求参数的形式传递给远程服务接口。
     * /product/skuList/{pageNum}/{pageSize}?keyword=xxx&brandId=1&category1Id=1&category2Id=2&category3Id=3
     */
    @GetMapping("/product/skuList/{pageNum}/{pageSize}")
    public R<TableDataInfo> skuList(
            @PathVariable("pageNum") Integer pageNum,
            @PathVariable("pageSize") Integer pageSize,
            @SpringQueryMap SkuQuery skuQuery,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source
    );


    @GetMapping("/product/getProductSku/{skuId}")
    public R<ProductSku> getProductSku(@PathVariable("skuId") Long skuId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping(value = "/product/getProduct/{id}")
    public R<Product> getProduct(@PathVariable("id") Long id, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping(value = "/product/getSkuPrice/{skuId}")
    public R<SkuPrice> getSkuPrice(@PathVariable("skuId") Long skuId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping(value = "/product/getProductDetails/{id}")
    public R<ProductDetails> getProductDetails(@PathVariable("id") Long id, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping(value = "/product/getSkuSpecValue/{id}")
    public R<Map<String, Long>> getSkuSpecValue(@PathVariable("id") Long id, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping(value = "/product/getSkuStock/{skuId}")
    public R<SkuStockVo> getSkuStock(@PathVariable("skuId") Long skuId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);


    /**
     * 批量获取商品sku最新价格信
     *
     * @param skuIdList
     * @param source
     * @return
     */
    @PostMapping(value = "/product/getSkuPriceList")
    public R<List<SkuPrice>> getSkuPriceList(@RequestBody List<Long> skuIdList, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 校验和减库存
     */
    @GetMapping("/product/checkAndSubStock/{skuId}/{skuNum}")
    R<String> checkAndSubStock(@PathVariable("skuId") Long skuId, @PathVariable("skuNum") Integer skuNum, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 还原库存和销量
     */
    @PostMapping("/product/addStockAndSubSaleNum")
    R<String> addStockAndSubSaleNum(@RequestBody List<SkuLockVo> skuLockVoList, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}

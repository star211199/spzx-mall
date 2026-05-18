package com.spzx.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spzx.product.api.domain.vo.SkuLockVo;
import com.spzx.product.api.domain.vo.SkuPrice;
import com.spzx.product.api.domain.vo.SkuQuery;
import com.spzx.product.api.domain.vo.SkuStockVo;
import com.spzx.product.api.domain.Product;
import com.spzx.product.api.domain.ProductSku;
import com.spzx.product.api.domain.ProductDetails;

import java.util.List;
import java.util.Map;

/**
 * 商品Service接口
 */
public interface IProductService extends IService<Product> {


    /**
     * 查询商品列表
     *
     * @param product 商品
     * @return 商品集合
     */
    public List<Product> selectProductList(Product product);

    /**
     * 保存商品信息
     * @param product 表单数据
     * @return 成功结果
     */
    int insertProduct(Product product);

    /**
     * 根据id查询商品详情
     * @param id 商品id
     * @return 商品对象信息
     */
    Product selectProductById(Long id);

    /**
     * 修改商品信息
     * @param product 表单数据
     * @return 成功结果
     */
    int updateProduct(Product product);

    /**
     * 删除商品信息(逻辑删除)
     * @param ids 多个商品ID
     * @return 成功结果
     */
    int deleteProductByIds(Long[] ids);

    /**
     * 审核
     * @param id 商品id
     * @param auditStatus 审核状态  1 通过    -1 拒绝
     */
    void updateAuditStatus(Long id, Integer auditStatus);

    /**
     * 上下架
     * @param id 商品id
     * @param status 状态  1 上架    -1 下架
     */
    void updateStatus(Long id, Integer status);

    /**
     * 畅销商品前20
     * @return 商品集合
     */
    List<ProductSku> getTopSale();

    /**
     * 列表查询
     * @param skuQuery
     * @return
     */
    List<ProductSku> skuList(SkuQuery skuQuery);



    //----详情 start------------------------------
    ProductSku getProductSku(Long skuId);

    Product getProduct(Long id);

    SkuPrice getSkuPrice(Long skuId);

    ProductDetails getProductDetails(Long id);

    Map<String, Long> getSkuSpecValue(Long id);

    SkuStockVo getSkuStock(Long skuId);
    //----详情 end------------------------------

    public List<SkuPrice> getSkuPriceList(List<Long> skuIdList);

    String checkAndSubStock(Long skuId, Integer skuNum);

    String addStockAndSubSaleNum(List<SkuLockVo> skuLockVoList);
}
package com.spzx.product.controller;

import com.github.pagehelper.PageHelper;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.exception.ServiceException;
import com.spzx.common.core.web.controller.BaseController;
import com.spzx.common.core.web.domain.AjaxResult;
import com.spzx.common.core.web.page.TableDataInfo;
import com.spzx.common.security.annotation.InnerAuth;
import com.spzx.common.security.utils.SecurityUtils;
import com.spzx.product.api.domain.ProductSku;
import com.spzx.product.api.domain.vo.SkuLockVo;
import com.spzx.product.api.domain.vo.SkuPrice;
import com.spzx.product.api.domain.vo.SkuQuery;
import com.spzx.product.api.domain.vo.SkuStockVo;
import com.spzx.product.api.domain.Product;
import com.spzx.product.api.domain.ProductDetails;
import com.spzx.product.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品Controller
 */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/product")
public class
ProductController extends BaseController {

    @Autowired
    private IProductService productService;

    /**
     * 查询商品列表
     */
    @Operation(summary = "查询商品列表")
    @GetMapping("/list")
    public TableDataInfo list(Product product) {
        startPage();
        List<Product> list = productService.selectProductList(product);
        return getDataTable(list);
    }


    /**
     * 新增商品
     *
     * @param product
     * @return
     */
    @Operation(summary = "新增商品")
    @PostMapping
    public AjaxResult add(@RequestBody Product product) {
        product.setCreateBy(SecurityUtils.getUsername());
        return toAjax(productService.insertProduct(product));
    }


    /**
     * 获取商品详细信息
     */
    @Operation(summary = "获取商品详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(productService.selectProductById(id));
    }


    /**
     * 修改商品
     *
     * @param product
     * @return
     */
    @Operation(summary = "修改商品")
    @PutMapping
    public AjaxResult edit(@RequestBody Product product) {
        product.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(productService.updateProduct(product));
    }


    /**
     * 删除商品
     *
     * @param ids
     * @return
     */
    @Operation(summary = "删除商品")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(productService.deleteProductByIds(ids));
    }


    @Operation(summary = "商品审核")
    @GetMapping("updateAuditStatus/{id}/{auditStatus}")
    public AjaxResult updateAuditStatus(@PathVariable Long id, @PathVariable Integer auditStatus) {
        productService.updateAuditStatus(id, auditStatus);
        return success();
    }


    @Operation(summary = "更新上下架状态")
    @GetMapping("updateStatus/{id}/{status}")
    public AjaxResult updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        productService.updateStatus(id, status);
        return success();
    }


    //========以下接口都是给前台系统使用====================================================================


    @InnerAuth
    @Operation(summary = "获取销量好的sku")
    @GetMapping("getTopSale")
    public R<List<ProductSku>> getTopSale() {
        return R.ok(productService.getTopSale());
    }


    // @ModelAttribute 注解作用：将请求参数封装在指定bean对象中。默认可以省略。
    @InnerAuth
    @Operation(summary = "列表查询")
    @GetMapping("/skuList/{pageNum}/{pageSize}")
    public R<TableDataInfo> skuList(
            @Parameter(name = "pageNum", required = true, description = "页码")
            @PathVariable("pageNum") Integer pageNum,
            @Parameter(name = "pageSize", required = true, description = "每页条数")
            @PathVariable("pageSize") Integer pageSize,
            /*@ModelAttribute*/ SkuQuery skuQuery
    ) {
        PageHelper.startPage(pageNum, pageSize);
        List<ProductSku> skuList = productService.skuList(skuQuery);
        TableDataInfo dataTable = getDataTable(skuList);
        return R.ok(dataTable);
    }


    //----详情 start------------------------------
    @Operation(summary = "获取商品sku信息")
    @InnerAuth
    @GetMapping(value = "/getProductSku/{skuId}")
    public R<ProductSku> getProductSku(@PathVariable("skuId") Long skuId) {
        return R.ok(productService.getProductSku(skuId));
    }

    @Operation(summary = "获取商品信息")
    @InnerAuth
    @GetMapping(value = "/getProduct/{id}")
    public R<Product> getProduct(@PathVariable("id") Long id) {
        return R.ok(productService.getProduct(id));
    }

    @Operation(summary = "获取商品sku最新价格信息")
    @InnerAuth
    @GetMapping(value = "/getSkuPrice/{skuId}")
    public R<SkuPrice> getSkuPrice(@PathVariable("skuId") Long skuId) {
        return R.ok(productService.getSkuPrice(skuId));
    }

    @Operation(summary = "获取商品详细信息")
    @InnerAuth
    @GetMapping(value = "/getProductDetails/{id}")
    public R<ProductDetails> getProductDetails(@PathVariable("id") Long id) {
        return R.ok(productService.getProductDetails(id));
    }

    @Operation(summary = "获取商品sku规则详细信息")
    @InnerAuth
    @GetMapping(value = "/getSkuSpecValue/{id}")
    public R<Map<String, Long>> getSkuSpecValue(@PathVariable("id") Long id) {
        return R.ok(productService.getSkuSpecValue(id));
    }

    @Operation(summary = "获取商品sku库存信息")
    @InnerAuth
    @GetMapping(value = "/getSkuStock/{skuId}")
    public R<SkuStockVo> getSkuStock(@PathVariable("skuId") Long skuId) {
        return R.ok(productService.getSkuStock(skuId));
    }
    //----详情 end------------------------------
    @InnerAuth
    @Operation(summary = "校验库存和减库存")
    @GetMapping("/checkAndSubStock/{skuId}/{skuNum}")
    public R<String> checkAndSubStock(@PathVariable Long skuId,@PathVariable Integer skuNum) {
        // 调用checkAndSubStock中的方法校验和减库存
        String msg = productService.checkAndSubStock(skuId, skuNum);
        return R.ok(msg);
    }
}
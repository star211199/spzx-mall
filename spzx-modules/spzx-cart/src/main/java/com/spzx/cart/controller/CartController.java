package com.spzx.cart.controller;

import com.spzx.cart.api.domain.CartInfo;
import com.spzx.cart.service.ICartService;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.web.controller.BaseController;
import com.spzx.common.core.web.domain.AjaxResult;
import com.spzx.common.security.annotation.InnerAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "购物车接口")
@RestController
@RequestMapping
public class CartController extends BaseController {

    @Autowired
    private ICartService cartService;

    @Operation(summary = "添加商品到购物车")
    @GetMapping("/addToCart/{skuId}/{num}")
    public AjaxResult addToCart(@PathVariable Long skuId, @PathVariable Integer num) {
        // 调用Iservice中的添加购物车方法
        cartService.add2Cart(skuId, num);
        return AjaxResult.success();
    }

    @Operation(summary = "展示购物车信息")
    @GetMapping("/cartList")
    public AjaxResult cartList() {
        List<CartInfo> cartList = cartService.cartList();
        return success(cartList);
    }

    @Operation(summary = "更新购物项状态")
    @GetMapping("/checkCart/{skuId}/{status}")
    public AjaxResult checkCart(@PathVariable Long skuId, @PathVariable Integer status) {
        cartService.updateCartInfoStatus(skuId,status);
        return success();
    }

    @Operation(summary = "更新所有购物项的状态")
    @GetMapping("/allCheckCart/{status}")
    public AjaxResult allCheckCart(@PathVariable Integer status) {
        cartService.updateAllCartInfoStatus(status);
        return success();
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/deleteCart/{skuId}")
    public AjaxResult deleteCart(@PathVariable Long skuId) {
        cartService.deleteCart(skuId);
        return success();
    }

    @Operation(summary = "清空购物车")
    @GetMapping("/clearCart")
    public AjaxResult clearCart() {
        cartService.clearCart();
        return success();
    }

    @InnerAuth
    @Operation(summary = "供其他微服务远程调用查询购物车中已选中的商品")
    @GetMapping("/getCheckedCartInfo")
    public R<List<CartInfo>> getCheckedCartInfo() {
        List<CartInfo> cartInfoList = cartService.getCheckedCartInfo();
        return R.ok(cartInfoList);
    }

    @InnerAuth
    @Operation(summary = "供微服务内部远程调用清除选中的购物项的方法")
    @GetMapping("/clearCheckedCartInfo")
    public R<Void> clearCheckedCartInfo() {
        cartService.clearCheckedCartInfo();
        return R.ok();
    }

}
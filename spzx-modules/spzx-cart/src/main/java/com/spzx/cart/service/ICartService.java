package com.spzx.cart.service;

import com.spzx.cart.api.domain.CartInfo;

import java.util.List;

public interface ICartService {

    void add2Cart(Long skuId, Integer num);


    List<CartInfo> cartList();

    void updateCartInfoStatus(Long skuId, Integer status);

    void updateAllCartInfoStatus(Integer status);

    void deleteCart(Long skuId);

    void clearCart();

    List<CartInfo> getCheckedCartInfo();

    /**
     * 当购买完商品后，清空Redis购物车
     */
    void clearCheckedCartInfo();
}

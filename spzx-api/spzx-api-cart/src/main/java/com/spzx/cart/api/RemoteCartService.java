package com.spzx.cart.api;

import com.spzx.cart.api.domain.CartInfo;
import com.spzx.cart.api.factory.RemoteCartFallbackFactory;
import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.constant.ServiceNameConstants;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.web.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(contextId = "remoteCartService",
        value = ServiceNameConstants.CART_SERVICE,
        fallbackFactory = RemoteCartFallbackFactory.class)
public interface RemoteCartService {

    @GetMapping("/getCheckedCartInfo")
    R<List<CartInfo>> getCheckedCartInfo(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/clearCheckedCartInfo")
    R<Void> clearCheckedCartInfo(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
package com.spzx.order.api;

import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.constant.ServiceNameConstants;
import com.spzx.common.core.domain.R;
import com.spzx.order.api.domain.OrderInfo;
import com.spzx.order.api.factory.RemoteOrderInfoFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "remoteUserInfoService", value = ServiceNameConstants.ORDER_SERVICE, fallbackFactory = RemoteOrderInfoFallbackFactory.class)
public interface RemoteOrderInfoService {
    /**
     * 根据订单号获取订单详情
     * @param orderNo
     * @param source
     * @return
     */
    @GetMapping("/orderInfo/getOrderInfoByOrderNo/{orderNo}")
    R<OrderInfo> getOrderInfoByOrderNo(@PathVariable("orderNo") String orderNo, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

}
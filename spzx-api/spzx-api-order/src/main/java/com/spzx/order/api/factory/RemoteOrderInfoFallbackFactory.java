package com.spzx.order.api.factory;

import com.spzx.common.core.domain.R;
import com.spzx.order.api.RemoteOrderInfoService;
import com.spzx.order.api.domain.OrderInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteOrderInfoFallbackFactory implements FallbackFactory<RemoteOrderInfoService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteOrderInfoFallbackFactory.class);

    @Override
    public RemoteOrderInfoService create(Throwable throwable) {
        log.error("订单服务调用失败:{}", throwable.getMessage());
        return new RemoteOrderInfoService() {
            @Override
            public R<OrderInfo> getOrderInfoByOrderNo(String orderNo, String source) {
                return R.fail("查询订单信息失败，失败的原因是：" + throwable.getMessage());
            }
        };
    }
}
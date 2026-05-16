package com.spzx.cart.api.factory;

import com.spzx.cart.api.RemoteCartService;
import com.spzx.common.core.constant.ServiceNameConstants;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.web.domain.AjaxResult;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteCartFallbackFactory implements FallbackFactory<RemoteCartService> {

    private Logger log = LoggerFactory.getLogger(RemoteCartFallbackFactory.class);

    @Override
    public RemoteCartService create(Throwable throwable) {

        log.error("远程调用服务【{}】出现降级", ServiceNameConstants.CART_SERVICE);

        return new RemoteCartService() {
            @Override
            public R getCheckedCartInfo(String source) {
                return R.fail("远程调用购物车微服务的获取选中的商品失败，失败的原因是：" + throwable.getMessage());
            }

            @Override
            public R<Void> clearCheckedCartInfo(String source) {
                return R.fail("远程调用购物车微服务的清空购物车方法失败，失败的原因是：" + throwable.getMessage());
            }
        };
    }
}

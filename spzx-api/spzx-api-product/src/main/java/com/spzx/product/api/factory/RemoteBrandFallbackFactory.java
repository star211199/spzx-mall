package com.spzx.product.api.factory;

import com.spzx.common.core.constant.ServiceNameConstants;
import com.spzx.common.core.domain.R;
import com.spzx.product.api.RemoteBrandService;
import com.spzx.product.api.domain.Brand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

//@Slf4j
@Component
public class RemoteBrandFallbackFactory implements FallbackFactory<RemoteBrandService> {
    // 根据当前类创建一个专属日志实例
    private static final Logger log = LoggerFactory.getLogger(RemoteBrandFallbackFactory.class);

    @Override
    public RemoteBrandService create(Throwable throwable) {
        log.error("商品服务【{}】调用失败:{}", ServiceNameConstants.PRODUCT_SERVICE, throwable.getMessage());

        return new RemoteBrandService() {
            @Override
            public R<List<Brand>> getBrandAllList(String source) {
                return R.fail("获取全部品牌失败:" + throwable.getMessage());
            }
        };
    }
}
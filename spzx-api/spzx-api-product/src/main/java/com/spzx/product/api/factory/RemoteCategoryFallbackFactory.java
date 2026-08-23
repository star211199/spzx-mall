package com.spzx.product.api.factory;

import com.spzx.common.core.constant.ServiceNameConstants;
import com.spzx.common.core.domain.R;
import com.spzx.product.api.RemoteCategoryService;
import com.spzx.product.api.domain.vo.CategoryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;

/**
 * 商品服务降级处理
 */
@Slf4j
//@Component
public class RemoteCategoryFallbackFactory implements FallbackFactory<RemoteCategoryService> {
    @Override
    public RemoteCategoryService create(Throwable cause) {

        log.error("远程服务【" + ServiceNameConstants.PRODUCT_SERVICE + "】调用出现问题了");

        return new RemoteCategoryService() {
            @Override
            public R<List<CategoryVo>> getOneCategory(String resource) {
                return R.fail("远程获取一级分类失败,降级处理");
            }

            @Override
            public R<List<CategoryVo>> tree(String resource) {
                return R.fail("远程获取三级分类失败,降级处理");
            }
        };
    }
}

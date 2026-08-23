package com.spzx.product.api;

import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.constant.ServiceNameConstants;
import com.spzx.common.core.domain.R;
import com.spzx.product.api.domain.vo.CategoryVo;
import com.spzx.product.api.factory.RemoteCategoryFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * OpenFeign远程调用接口
 * contextId = "remoteCategoryService" IOC容器中Bean对象的id
 * value = ServiceNameConstants.PRODUCT_SERVICE  指定远程调用的服务名称，通过这个名称到注册中心查找服务，拉取服务列表进行远程调用。
 * fallbackFactory = RemoteCategoryFallbackFactory.class  设置降级处理类。远程调用出问题了，都找降级处理类返回兜底结果。
 * 我们项目中降级处理用的是OpenFeign组件提供的降级工程类。不是Sentinel。
 */
@FeignClient(contextId = "remoteCategoryService", value = ServiceNameConstants.PRODUCT_SERVICE,
        fallbackFactory = RemoteCategoryFallbackFactory.class)
public interface RemoteCategoryService {

    //声明远程接口：
    //由于远程接口声明@InnerAuth注解，切面需要获取请求头： from-source = "inner"
    //如何携带多个请求头？  参数类型：可以  Map or MultiValueMap or HttpHeaders
    // @RequestHeader(SecurityConstants.FROM_SOURCE) String resource 表示含义，就是告诉OpenFeign组件发送远程调用请求时，将参数以请求头的方式携带过去。
    @GetMapping("/category/getOneCategory")
    public R<List<CategoryVo>> getOneCategory(@RequestHeader(SecurityConstants.FROM_SOURCE) String resource);

    /**
     * 查询三级分类
     *
     * @param resource
     * @return
     */
    @GetMapping(value = "/category/tree")
    public R<List<CategoryVo>> tree(@RequestHeader(SecurityConstants.FROM_SOURCE) String resource);
}

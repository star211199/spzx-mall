package com.spzx.order.controller;

import com.github.pagehelper.PageHelper;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.utils.poi.ExcelUtil;
import com.spzx.common.core.web.controller.BaseController;
import com.spzx.common.core.web.domain.AjaxResult;
import com.spzx.common.core.web.page.TableDataInfo;
import com.spzx.common.log.annotation.Log;
import com.spzx.common.log.enums.BusinessType;
import com.spzx.common.security.annotation.InnerAuth;
import com.spzx.common.security.annotation.RequiresLogin;
import com.spzx.common.security.annotation.RequiresPermissions;
import com.spzx.order.api.domain.OrderInfo;
import com.spzx.order.domain.vo.OrderForm;
import com.spzx.order.domain.vo.TradeVo;
import com.spzx.order.receiver.OrderReceiver;
import com.spzx.order.service.IOrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单接口管理")
@RestController
@RequestMapping("/orderInfo")
public class OrderInfoController extends BaseController {
    @Autowired
    private IOrderInfoService orderInfoService;

    @Operation(summary = "供远程微服务内部调用根据订单号查询订单信息的方法")
    @GetMapping("/getOrderInfoByOrderNo/{orderNo}")
    public R<OrderInfo> getOrderInfoByOrderNo(@PathVariable String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(orderNo);
        return R.ok(orderInfo);
    }

    @Operation(summary = "取消订单")
    @GetMapping("/cancelOrder/{orderId}")
    public AjaxResult cancelOrder(@PathVariable Long orderId) {
        orderInfoService.cancelOrder(orderId,1);
        return success();
    }

    @Operation(summary = "获取我的订单信息")
    @GetMapping("/userOrderInfoList/{pageNum}/{pageSize}")
    public TableDataInfo myOrderInfoList(@PathVariable Integer pageNum, @PathVariable Integer pageSize, String orderStatus) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        List<OrderInfo> orderInfoList = orderInfoService.myOrderInfoList(orderStatus);
        return getDataTable(orderInfoList);
    }

    @Operation(summary = "获取订单信息")
    @GetMapping("/getOrderInfo/{orderId}")
    public AjaxResult getOrderInfo(@PathVariable("orderId") Long orderId) {
        OrderInfo byId = orderInfoService.getById(orderId);
        return success(byId);
    }

    @Operation(summary = "提交订单")
    @PostMapping("/submitOrder")
    public AjaxResult submitOrder(@RequestBody OrderForm orderForm) {
        // 先生成订单表，返回订单的Id，再更新订单项表
        Long orderId = orderInfoService.submitOrder(orderForm);
        return success(orderId);
    }

    @Operation(summary = "去结算")
    @GetMapping("/trade")
    public AjaxResult trade(){
        TradeVo tradeVo = orderInfoService.trade();
        return success(tradeVo);
    }

    @Operation(summary = "直接购买某一件商品")
    @GetMapping("/buy/{skuId}")
    public AjaxResult buy(@PathVariable("skuId") Long skuId){
        TradeVo tradeVo = orderInfoService.buy(skuId);
        return success(tradeVo);
    }

    /**
     * 查询订单列表
     */
    @Operation(summary = "查询订单列表")
    @RequiresPermissions("user:orderInfo:list")
    @GetMapping("/list")
    public TableDataInfo list(OrderInfo orderInfo) {
        startPage();
        List<OrderInfo> list = orderInfoService.selectOrderInfoList(orderInfo);
        return getDataTable(list);
    }

    /**
     * 导出订单列表
     */
    @Operation(summary = "导出订单列表")
    @RequiresPermissions("user:orderInfo:export")
    @Log(title = "订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderInfo orderInfo) {
        List<OrderInfo> list = orderInfoService.selectOrderInfoList(orderInfo);
        ExcelUtil<OrderInfo> util = new ExcelUtil<OrderInfo>(OrderInfo.class);
        util.exportExcel(response, list, "订单数据");
    }

    /**
     * 获取订单详细信息
     */
    @Operation(summary = "获取订单详细信息")
    @RequiresPermissions("user:orderInfo:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(orderInfoService.selectOrderInfoById(id));
    }

}
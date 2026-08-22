package com.spzx.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.spzx.common.core.web.controller.BaseController;
import com.spzx.common.core.web.domain.AjaxResult;
import com.spzx.common.security.annotation.RequiresLogin;
import com.spzx.payment.configure.AlipayConfig;
import com.spzx.payment.service.IAlipayService;
import com.spzx.payment.service.IPaymentInfoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alipay")
public class AlipayController extends BaseController {

    @Autowired
    private IAlipayService alipayService;

    @Autowired
    private IPaymentInfoService paymentInfoService;

    @Operation(summary = "调用支付宝获取打开支付宝表单的方法")
    @GetMapping("/submitAlipay/{orderNo}")
    public AjaxResult submitAlipay(@PathVariable("orderNo") String orderNo) {
        String form = alipayService.submitAlipay(orderNo);
        return success(form);
    }

    @Operation(summary = "支付宝回调通知支付结构的方法")
    @PostMapping("/callback/notify")
    public String notify(@RequestParam Map<String, String> params) {
        log.info("收到支付宝回调，参数：{}", params);
        try {
            // 验签
            boolean flag = AlipaySignature.rsaCheckV1(params, AlipayConfig.alipay_public_key, AlipayConfig.charset,AlipayConfig.sign_type);
            if (flag) {
                // 判断交易是否成功
                String status = params.get("trade_status");
                // 如果验签成功，更新payment_info
                if ("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status)) {
                    alipayService.updatePaymentStatus(params);
                    log.info("支付宝回调处理成功");
                    return "success";
                }
            }

        } catch (AlipayApiException e) {
            log.error("支付宝回调处理异常", e);
            throw new RuntimeException(e);
        }
        return "fail";
    }

}
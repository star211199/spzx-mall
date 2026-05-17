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
@Controller
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

}
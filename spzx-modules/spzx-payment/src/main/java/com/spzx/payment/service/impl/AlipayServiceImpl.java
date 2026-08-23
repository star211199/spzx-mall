package com.spzx.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.domain.*;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.spzx.common.core.exception.ServiceException;
import com.spzx.common.rabbit.constant.MqConst;
import com.spzx.common.rabbit.service.RabbitService;
import com.spzx.payment.configure.AlipayConfig;
import com.spzx.payment.domain.PaymentInfo;
import com.spzx.payment.service.IAlipayService;
import com.spzx.payment.service.IPaymentInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 支付宝支付服务实现类
 * 增强支付安全性：签名验证、幂等性检查、金额校验
 */
@Slf4j
@Service
public class AlipayServiceImpl implements IAlipayService {

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    IPaymentInfoService paymentInfoService;
    @Autowired
    private RabbitService rabbitService;

    @Override
    public String submitAlipay(String orderNo) {
        // 获取或者生成支付订单
        PaymentInfo paymentInfo = paymentInfoService.addOrGetPaymentInfo(orderNo);

        // 创建请求对象
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();

        // 设置商户订单号
        model.setOutTradeNo(orderNo);

        // 设置订单总金额
        model.setTotalAmount(paymentInfo.getAmount().toString());

        // 设置订单标题
        model.setSubject(paymentInfo.getContent());

        // 设置产品码
        model.setProductCode("QUICK_WAP_WAY");

        // 设置同步调用网站支付成功页面的地址
        request.setReturnUrl(AlipayConfig.return_payment_url);
        // 设置用户支付成功后让支付宝异步调用通知支付的地址
        request.setNotifyUrl(AlipayConfig.notify_payment_url);


        request.setBizModel(model);
        // 第三方代调用模式下请设置app_auth_token
        // request.putOtherTextParam("app_auth_token", "<-- 请填写应用授权令牌 -->");

        try {
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(request, "POST");
            String formUrl = response.getBody();

            if (response.isSuccess()) {
                log.info("支付宝支付页面生成成功，订单号：{}", orderNo);
                return formUrl;
            } else {
                log.error("支付宝支付页面生成失败，订单号：{}，错误信息：{}", orderNo, response.getMsg());
                throw new ServiceException("支付页面生成失败：" + response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝支付接口异常，订单号：{}", orderNo, e);
            throw new ServiceException("支付接口调用异常");
        }
    }

    /**
     * 更新支付状态
     * 增强安全性：签名验证、幂等性检查、金额校验
     *
     * @param params 支付宝回调参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentStatus(Map<String, String> params) {
        // 1. 验证签名（防止伪造回调）
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    AlipayConfig.alipay_public_key,
                    "UTF-8",
                    "RSA2"
            );
            if (!signVerified) {
                log.error("支付宝回调签名验证失败，参数：{}", params);
                throw new ServiceException("签名验证失败");
            }
        } catch (AlipayApiException e) {
            log.error("支付宝签名验证异常", e);
            throw new ServiceException("签名验证异常");
        }

        log.info("支付宝回调签名验证通过");

        // 获取订单号
        String orderNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmountStr = params.get("total_amount");

        // 2. 幂等性检查：检查是否已经处理过该订单
        PaymentInfo existingPayment = paymentInfoService.getOne(
                new LambdaQueryWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, orderNo)
                        .eq(PaymentInfo::getPaymentStatus, "1")
        );
        if (existingPayment != null) {
            log.info("订单已支付成功，跳过重复处理，订单号：{}", orderNo);
            return;
        }

        // 3. 只处理支付成功的状态
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.warn("非支付成功状态，跳过处理，订单号：{}，状态：{}", orderNo, tradeStatus);
            return;
        }

        // 4. 金额校验（可选，防止金额篡改）
        if (totalAmountStr != null && !totalAmountStr.isEmpty()) {
            PaymentInfo paymentInfo = paymentInfoService.getOne(
                    new LambdaQueryWrapper<PaymentInfo>()
                            .eq(PaymentInfo::getOrderNo, orderNo)
            );
            if (paymentInfo != null) {
                BigDecimal expectedAmount = paymentInfo.getAmount();
                BigDecimal actualAmount = new BigDecimal(totalAmountStr);
                if (expectedAmount.compareTo(actualAmount) != 0) {
                    log.error("支付金额不匹配，订单号：{}，期望金额：{}，实际金额：{}",
                            orderNo, expectedAmount, actualAmount);
                    throw new ServiceException("支付金额不匹配");
                }
            }
        }

        // 5. 更新支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        // 更新交易编号
        String tradeNo = params.get("trade_no");
        paymentInfo.setTradeNo(tradeNo);
        // 更新交易状态
        paymentInfo.setPaymentStatus("1");
        // 更新时间
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(params.toString());
        paymentInfo.setUpdateTime(new Date());

        // 带条件的更新（确保幂等性）
        boolean updated = paymentInfoService.update(
                paymentInfo,
                new LambdaUpdateWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, orderNo)
                        .ne(PaymentInfo::getPaymentStatus, "1") // 只更新未支付的订单
        );

        if (updated) {
            log.info("支付状态更新成功，订单号：{}", orderNo);
            // 6. 发送消息通知订单服务更新订单状态
            rabbitService.sendMessage(MqConst.EXCHANGE_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, orderNo);
        } else {
            log.warn("支付状态更新失败（可能已处理），订单号：{}", orderNo);
        }
    }
}

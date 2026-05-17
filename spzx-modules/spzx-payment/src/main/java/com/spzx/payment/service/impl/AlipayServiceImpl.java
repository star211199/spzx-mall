package com.spzx.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.domain.ExtUserInfo;
import com.alipay.api.domain.ExtendParams;
import com.alipay.api.domain.GoodsDetail;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.spzx.payment.configure.AlipayConfig;
import com.spzx.payment.domain.PaymentInfo;
import com.spzx.payment.service.IAlipayService;
import com.spzx.payment.service.IPaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlipayServiceImpl implements IAlipayService {

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    IPaymentInfoService paymentInfoService;

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
            // 如果需要返回GET请求，请使用
            // AlipayTradeWapPayResponse response = alipayClient.pageExecute(request, "GET");
            String formUrl = response.getBody();
            System.out.println(formUrl);

            if (response.isSuccess()) {
                System.out.println("调用成功");
                return formUrl;
            } else {
                System.out.println("调用失败");
            }
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        return "";
    }
}

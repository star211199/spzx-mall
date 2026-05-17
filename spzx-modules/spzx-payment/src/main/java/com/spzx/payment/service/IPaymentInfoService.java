package com.spzx.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spzx.payment.domain.PaymentInfo;

/**
 * 付款信息Service接口
 */
public interface IPaymentInfoService extends IService<PaymentInfo> {

    /**
     * 添加或获取支付记录的方法
     */
    PaymentInfo addOrGetPaymentInfo(String orderNo);
}
package com.spzx.payment.service;

import java.util.Map;

public interface IAlipayService {

    String submitAlipay(String orderNo);

    void updatePaymentStatus(Map<String, String> params);
}

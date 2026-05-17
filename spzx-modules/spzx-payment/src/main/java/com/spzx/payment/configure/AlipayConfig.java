package com.spzx.payment.configure;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Value("${alipay.alipay_url}")
    private String alipay_url;

    @Value("${alipay.app_private_key}")
    private String app_private_key;

    @Value("${alipay.app_id}")
    private String app_id;


    public final static String format="json";
    public final static String charset="utf-8";
    public final static String sign_type="RSA2";


    public static String return_payment_url;

    public static  String notify_payment_url;

    public static  String return_order_url;

    public static  String alipay_public_key;

    @Value("${alipay.alipay_public_key}")
    public  void setAlipay_public_key(String alipay_public_key) {
        AlipayConfig.alipay_public_key = alipay_public_key;
    }

    @Value("${alipay.return_payment_url}")
    public   void setReturn_url(String return_payment_url) {
        AlipayConfig.return_payment_url = return_payment_url;
    }

    @Value("${alipay.notify_payment_url}")
    public   void setNotify_url(String notify_payment_url) {
        AlipayConfig.notify_payment_url = notify_payment_url;
    }

//    @Value("${alipay.return_order_url}")
//    public   void setReturn_order_url(String return_order_url) {
//        AlipayConfig.return_order_url = return_order_url;
//    }

    @Bean
    public AlipayClient alipayClient(){
        AlipayClient alipayClient=new DefaultAlipayClient(alipay_url,app_id,app_private_key,format,charset, alipay_public_key,sign_type );
        return alipayClient;
    }

}
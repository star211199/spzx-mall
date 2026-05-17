package com.spzx.common.rabbit.constant;

public class MqConst {

    /**
     * 测试
     */
    public static final String EXCHANGE_TEST = "spzx.test";
    public static final String ROUTING_TEST = "spzx.test";
    public static final String ROUTING_CONFIRM = "spzx.confirm";
    //队列
    public static final String QUEUE_TEST = "spzx.test";
    public static final String QUEUE_CONFIRM = "spzx.confirm";

    /**
     * 库存
     */
    public static final String EXCHANGE_PRODUCT = "spzx.product";
    public static final String ROUTING_UNLOCK = "spzx.unlock";
    public static final String ROUTING_MINUS = "spzx.minus";
    //队列
    public static final String QUEUE_UNLOCK = "spzx.unlock";
    public static final String QUEUE_MINUS = "spzx.minus";

    /**
     * 支付
     */
    public static final String EXCHANGE_PAYMENT_PAY = "spzx.payment";
    public static final String ROUTING_PAYMENT_PAY = "spzx.payment.pay";
    public static final String ROUTING_PAYMENT_CLOSE = "spzx.payment.close";
    ;
    public static final String QUEUE_PAYMENT_PAY = "spzx.payment.pay";
    public static final String QUEUE_PAYMENT_CLOSE = "queue.payment.close";


    /**
     * 取消订单延迟消息
     */
    public static final String EXCHANGE_CANCEL_ORDER = "spzx.cancel.order";
    public static final String ROUTING_CANCEL_ORDER = "spzx.cancel.order";
    public static final String QUEUE_CANCEL_ORDER = "spzx.cancel.order";
    //public static final Integer CANCEL_ORDER_DELAY_TIME = 15 * 60;
    public static final Integer CANCEL_ORDER_DELAY_TIME = 30*60000; //TODO 临时


}
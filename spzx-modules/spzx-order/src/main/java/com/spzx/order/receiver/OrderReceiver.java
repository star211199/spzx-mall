package com.spzx.order.receiver;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rabbitmq.client.Channel;
import com.spzx.common.rabbit.constant.MqConst;
import com.spzx.order.api.domain.OrderInfo;
import com.spzx.order.service.IOrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

@Slf4j
@Component
public class OrderReceiver {

    @Autowired
    private IOrderInfoService orderInfoService;

    /**
     * 订单超时未支付，自动取消订单并解锁库存
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_CANCEL_ORDER),
            exchange = @Exchange(value = MqConst.EXCHANGE_CANCEL_ORDER,
                    type = "x-delayed-type",
                    arguments = @Argument(name = "x-delayed-type", value = "direct")),
            key = MqConst.ROUTING_CANCEL_ORDER))
    public void receiverMessageCancelOrder(Long orderId, Message message, Channel channel) {

        // 获取消息的标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 如果订单还未支付，就取消订单
            orderInfoService.cancelOrder(orderId, 0);
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            try {
                // 如果消息消费失败，重新放回队列
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * 支付成功，更新订单状态为已支付
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY),
            exchange = @Exchange(value = MqConst.EXCHANGE_PAYMENT_PAY),
            key = MqConst.ROUTING_PAYMENT_PAY))
    public void receiverMessageUpdateOrder(String orderNo, Message message, Channel channel) {

        // 获取消息的标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 根据orderNo更新订单状态，仅当订单处于待支付状态时才更新（幂等，避免覆盖已取消订单）
            boolean updated = orderInfoService.update(new LambdaUpdateWrapper<OrderInfo>()
                    .set(OrderInfo::getUpdateTime, new Date())
                    .set(OrderInfo::getOrderStatus, 1)
                    .eq(OrderInfo::getOrderNo, orderNo)
                    .eq(OrderInfo::getOrderStatus, 0));
            if (updated) {
                log.info("支付成功消息消费成功，订单已更新为已支付，订单号：{}", orderNo);
            } else {
                log.warn("订单当前不是待支付状态，跳过支付成功更新，订单号：{}", orderNo);
            }
            // 成功确认消息
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            try {
                // 消息消费失败，重新放回队列
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }
}

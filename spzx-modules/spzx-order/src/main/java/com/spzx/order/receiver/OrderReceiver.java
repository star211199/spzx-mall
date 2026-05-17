package com.spzx.order.receiver;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQImpl;
import com.spzx.common.rabbit.constant.MqConst;
import com.spzx.common.rabbit.service.RabbitService;
import com.spzx.order.api.domain.OrderInfo;
import com.spzx.order.mapper.OrderInfoMapper;
import com.spzx.order.service.IOrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.BindStatus;

import javax.naming.Name;
import java.io.IOException;
import java.security.Key;
import java.security.MessageDigest;

@Slf4j
@Component
public class OrderReceiver {

    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private RabbitService rabbitService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_CANCEL_ORDER),
            exchange = @Exchange(value = MqConst.EXCHANGE_CANCEL_ORDER,
                    type = "x-delayed-type",
                    arguments = @Argument(name = "x-delayed-type",value = "direct")),
            key = MqConst.ROUTING_CANCEL_ORDER))
    public void receiverMessage(Long orderId,Message message, Channel channel){

        // 获取消息的标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 获取消息，如果没有支付订单，就取消订单
//            String orderId = new String(message.getBody());
            // 更新的订单状态
//            orderInfoService.update(new LambdaUpdateWrapper<OrderInfo>().eq(OrderInfo::getId, orderId).eq(OrderInfo::getOrderStatus,0).set(OrderInfo::getOrderStatus,-1));
            orderInfoService.cancelOrder(orderId,0);
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            try {
                // 如果消息消费失败，重新放回队列
                channel.basicNack(deliveryTag,false,true);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }

    }
}
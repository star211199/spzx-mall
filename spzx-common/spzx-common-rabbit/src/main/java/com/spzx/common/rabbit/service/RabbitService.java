package com.spzx.common.rabbit.service;

import com.spzx.common.rabbit.constant.MqConst;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送正常的消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        return true;
    }

    /**
     * 发送延迟消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message,msg ->{
            // 获取消息属性
            MessageProperties messageProperties = msg.getMessageProperties();
            // 设置延迟消息
            messageProperties.setDelay(MqConst.CANCEL_ORDER_DELAY_TIME);
            return msg;
        });
        return true;
    }

}
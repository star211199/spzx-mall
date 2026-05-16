package com.spzx.order.receiver;

import com.spzx.common.rabbit.constant.MqConst;
import com.spzx.common.rabbit.service.RabbitService;
import com.spzx.order.service.IOrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.BindStatus;

import javax.naming.Name;
import java.nio.channels.Channel;
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
    public void receiverMessage(Message message, Channel channel) {

    }
}
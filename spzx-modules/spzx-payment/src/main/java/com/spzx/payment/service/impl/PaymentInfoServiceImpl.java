package com.spzx.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.context.SecurityContextHolder;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.exception.ServiceException;
import com.spzx.common.rabbit.service.RabbitService;
import com.spzx.order.api.RemoteOrderInfoService;
import com.spzx.order.api.domain.OrderInfo;
import com.spzx.order.api.domain.OrderItem;
import com.spzx.payment.domain.PaymentInfo;
import com.spzx.payment.mapper.PaymentInfoMapper;
import com.spzx.payment.service.IPaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 付款信息Service业务层处理
 */
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements IPaymentInfoService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RemoteOrderInfoService remoteOrderInfoService;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public PaymentInfo addOrGetPaymentInfo(String orderNo) {
        // 首先根据订单号查询是有已经存在该支付订单
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, orderNo).eq(PaymentInfo::getPaymentStatus,0));
        if (paymentInfo != null) {
            return paymentInfo;
        }
        // 如果没有支付订单，创建支付订单
        paymentInfo = new PaymentInfo();
        // 设置订单号
        paymentInfo.setOrderNo(orderNo);
        // 设置用户Id
        paymentInfo.setUserId(SecurityContextHolder.getUserId());
        // 设置使用什么进行支付
        paymentInfo.setPayType(2);
        // 设置订单状态
        paymentInfo.setPaymentStatus("0");
        // 远程调用微服务，查询订单信息
        R<OrderInfo> orderInfoByOrderNoR = remoteOrderInfoService.getOrderInfoByOrderNo(orderNo, SecurityConstants.INNER);
        // 判断远程调用是否出现了异常
        if(R.FAIL == orderInfoByOrderNoR.getCode()){
            throw new ServiceException("远程调用微服务查询订单信息失败！");
        }
        OrderInfo orderInfo = orderInfoByOrderNoR.getData();
        // 设置交易的总金额
        paymentInfo.setAmount(orderInfo.getTotalAmount());
        // 设置交易内容
        StringBuffer sb = new StringBuffer();
        List<OrderItem> orderItemList = orderInfo.getOrderItemList();
        orderItemList.forEach(orderItem -> {
            if(sb.length() > 0) {
                sb.append(";");
            }
            sb.append(orderItem.getSkuName());
        });
        paymentInfo.setContent(sb.toString());

        // 插入支付记录
        paymentInfoMapper.insert(paymentInfo);

        return paymentInfo;
    }
}
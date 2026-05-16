package com.spzx.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spzx.order.api.domain.OrderItem;
import com.spzx.order.mapper.OrderItemMapper;
import com.spzx.order.service.IOrderItemService;
import org.springframework.stereotype.Service;

/**
* ClassName: OrderItemServiceImpl
* Project: spzx-parent
* Description: 
* @Author 周冠全
* @Create 2026/5/16 21:28 
* @Version 1.0   
*/
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements IOrderItemService {
}

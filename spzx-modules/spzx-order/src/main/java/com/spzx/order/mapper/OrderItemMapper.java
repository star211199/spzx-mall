package com.spzx.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spzx.order.api.domain.OrderItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 订单项信息Mapper接口
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {


}
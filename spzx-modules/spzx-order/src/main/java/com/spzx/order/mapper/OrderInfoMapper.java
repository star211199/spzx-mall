package com.spzx.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spzx.order.api.domain.OrderInfo;
import com.spzx.order.api.domain.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper接口
 */

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 查询订单列表
     *
     * @param orderInfo 订单
     * @return 订单集合
     */
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo);

    /**
     * 获取用户的订单列表
     *
     * @param userId      用户Id
     * @param orderStatus 订单状态
     * @return 订单列表
     */
    List<OrderInfo> selectUserOrderInfoList(@Param("userId") Long userId, @Param("orderStatus") Integer orderStatus);

    List<OrderInfo> selectOrderInfoListByUserId(
            @Param("userId") Long userId,
            @Param("orderStatus") Integer orderStatus
    );

    // 辅助查询，根据orderId查询订单项
    List<OrderItem> selectOrderItemListByOrderId(Long orderId);
}
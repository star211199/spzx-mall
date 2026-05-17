package com.spzx.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.spzx.cart.api.RemoteCartService;
import com.spzx.cart.api.domain.CartInfo;
import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.context.SecurityContextHolder;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.exception.ServiceException;
import com.spzx.common.core.utils.StringUtils;
import com.spzx.common.core.web.domain.AjaxResult;
import com.spzx.common.rabbit.config.RabbitInitConfigApplicationListener;
import com.spzx.common.rabbit.constant.MqConst;
import com.spzx.common.rabbit.service.RabbitService;
import com.spzx.order.api.domain.OrderInfo;
import com.spzx.order.api.domain.OrderItem;
import com.spzx.order.domain.OrderLog;
import com.spzx.order.domain.vo.OrderForm;
import com.spzx.order.domain.vo.TradeVo;
import com.spzx.order.mapper.OrderInfoMapper;
import com.spzx.order.mapper.OrderItemMapper;
import com.spzx.order.mapper.OrderLogMapper;
import com.spzx.order.service.IOrderInfoService;
import com.spzx.order.service.IOrderItemService;
import com.spzx.product.api.RemoteProductService;
import com.spzx.product.api.domain.ProductSku;
import com.spzx.user.api.RemoteUserAddressService;
import com.spzx.user.domain.UserAddress;
import io.lettuce.core.OrderingReadFromAccessor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderInfoService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private RemoteCartService remoteCartService;

    @Autowired
    private IOrderItemService orderItemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RemoteProductService remoteProductService;

    @Autowired
    private RemoteUserAddressService remoteUserAddressService;

    @Autowired
    RabbitService rabbitService; //来自于公共模块：spzx-common-rabbit

    /**
     * 查询订单列表
     *
     * @param orderInfo 订单
     * @return 订单
     */
    @Override
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo) {
        return orderInfoMapper.selectOrderInfoList(orderInfo);
    }

    /**
     * 查询订单
     *
     * @param id 订单主键
     * @return 订单
     */
    @Override
    public OrderInfo selectOrderInfoById(Long id) {
        OrderInfo orderInfo = orderInfoMapper.selectById(id);
        List<OrderItem> orderItemList = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        orderInfo.setOrderItemList(orderItemList);
        return orderInfo;
    }

    @Override
    public TradeVo trade() {
        TradeVo tradeVo = new TradeVo();
        // 远程调用获取购物车中的商品
        R<List<CartInfo>> checkedCartInfo = remoteCartService.getCheckedCartInfo(SecurityConstants.INNER);
        // 判断远程调用是否失败
        if (R.FAIL == checkedCartInfo.getCode()) {
            throw new ServiceException("调用远程微服务失败");
        }
        // 购物车中选中的商品
        List<CartInfo> cartInfoList = checkedCartInfo.getData();
        // 使用流进行转换，将List<CartInfo> ==> List<OrderItem>
        List<OrderItem> orderItemList = new ArrayList<OrderItem>();

        // 如果不为空
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            orderItemList = cartInfoList.stream().map(cartInfo -> {
                OrderItem orderItem = new OrderItem();
                BeanUtils.copyProperties(cartInfo, orderItem);
                return orderItem;
            }).collect(Collectors.toList());
        }
        // 设置所选的商品
        tradeVo.setOrderItemList(orderItemList);

        // 商品总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        // 遍历商品
        for (CartInfo cartInfo : cartInfoList) {
            // 获取商品的价格
            BigDecimal cartPrice = cartInfo.getCartPrice();
            // 获取商品的数量
            Integer skuNum = cartInfo.getSkuNum();
            // 计算金额小计
            BigDecimal amount = cartPrice.multiply(new BigDecimal(skuNum));
            // 累加总金额
            totalAmount = totalAmount.add(amount);
        }
        tradeVo.setTotalAmount(totalAmount);

        // 使用UUID生成订单号
        String tradeNo = getTradeNo();
        tradeVo.setTradeNo(tradeNo);

        return tradeVo;
    }


    // 生成订单号，防止重复提交
    private String getTradeNo() {
        String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
        // 将订单号发到Redis中，并设置过期时间
        redisTemplate.opsForValue().set("user:trade:" + tradeNo, tradeNo, 5, TimeUnit.MINUTES);
        return tradeNo;
    }

    @Override
    public TradeVo buy(Long skuId) {
        TradeVo tradeVo = new TradeVo();
        // 远程调用获取商品的信息
        R<ProductSku> productSkuR = remoteProductService.getProductSku(skuId, SecurityConstants.INNER);
        // 判断远程是否有异常
        if (R.FAIL == productSkuR.getCode()) {
            throw new ServiceException("远程调用失败！");
        }
        ProductSku productSku = productSkuR.getData();
        OrderItem orderItem = new OrderItem();
        BeanUtils.copyProperties(productSku, orderItem);
        // 设置订单Id为null
        orderItem.setId(null);
        // 设置sku
        orderItem.setSkuId(skuId);
        // 设置商品价格
        orderItem.setSkuPrice(productSku.getSalePrice());
        // 设置商品的数量
        orderItem.setSkuNum(1);

        List<OrderItem> orderItemList = Arrays.asList(orderItem);
        tradeVo.setOrderItemList(orderItemList);

        // 设置金额
        tradeVo.setTotalAmount(productSku.getSalePrice());

        // 设置订单号
        String tradeNo = getTradeNo();
        tradeVo.setTradeNo(tradeNo);

        return tradeVo;
    }

    @Override
    public Long submitOrder(OrderForm orderForm) {
        // 1.TODO:订单号防止重复提交订单
        // 获取前端页面返回的订单号
        String tradeNo = orderForm.getTradeNo();
        // 后期Redis中的订单号
        String redisTradeNo = (String) redisTemplate.opsForValue().get("user:trade:" + tradeNo);
        // 判断两个订单号是否相等
        if (!tradeNo.equals(redisTradeNo)) {
            // 如果不相等，抛出异常
            throw new ServiceException("下单超时或重复提交订单");
        } else {
            // 如果相等就删除Redis中的订单号
            redisTemplate.delete("user:trade:" + tradeNo);
        }


        // 2.插入订单表
        OrderInfo orderInfo = new OrderInfo();
        // 表单提交的数据
        orderInfo.setFeightFee(orderForm.getFeightFee());
        orderInfo.setRemark(orderForm.getRemark());
        // 设置订单的状态
        orderInfo.setOrderStatus(0);
        // 设置用户Id
        orderInfo.setUserId(SecurityContextHolder.getUserId());
        // 设置用户名
        orderInfo.setCreateBy(SecurityContextHolder.getUserName());
        // 随机生成一个字符串作为订单号
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        orderInfo.setOrderNo(orderNo);
        // 设置订单的总金额
        List<OrderItem> orderItemList = orderForm.getOrderItemList();

        // 搜集变动信息
        StringBuffer sb = new StringBuffer();
        // 遍历订单项计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem orderItem : orderItemList) {
            // 获取商品的价格
            BigDecimal skuPrice = orderItem.getSkuPrice();
            // TODO:查询实时价格
            // 远程调用查询商品,获取sku信息
            R<ProductSku> productSkuR = remoteProductService.getProductSku(orderItem.getSkuId(), SecurityConstants.INNER);
            // 判断是否报错
            if (R.FAIL == productSkuR.getCode()) {
                throw new ServiceException("远程调用微服务查询sku信息失败!");
            }
            ProductSku productSku = productSkuR.getData();
            BigDecimal salePrice = productSku.getSalePrice();
            BigDecimal diff = skuPrice.subtract(salePrice);
            if (diff.intValue() > 0) {
                sb.append(orderItem.getSkuNum() + "涨价" + diff.doubleValue() + "元" + ",请去结算页面重新下单");
            }
            if (diff.intValue() < 0) {
                sb.append(orderItem.getSkuNum() + "降价" + diff.doubleValue() + "元" + ",请去结算页面重新下单");
            }

            // 获取商品的数量
            Integer skuNum = orderItem.getSkuNum();
            // TODO:校验库存和减库存
            R<String> stringR = remoteProductService.checkAndSubStock(orderItem.getSkuId(), orderItem.getSkuNum(), SecurityConstants.INNER);
            if (R.FAIL == stringR.getCode()) {
                throw new ServiceException("远程调用校验库存和减库存微服务失败");
            }
            // 获取失败的数据
            String msg = stringR.getData();
            if (StringUtils.hasText(msg)) {
                throw new ServiceException(msg);
            }
            // 计算金额小计
            BigDecimal amount = skuPrice.multiply(new BigDecimal(skuNum));
            // 累加总金额
            totalAmount = totalAmount.add(amount);
        }
        orderInfo.setTotalAmount(totalAmount);
        orderInfo.setOriginalTotalAmount(totalAmount);

        // 判断sb是否为空
        if (StringUtils.hasText(sb.toString())) {
            throw new ServiceException(sb.toString());
        }

        // 根据用户收货地址Id远程调用用户微服务获取用户收货地址
        R<UserAddress> userAddressR = remoteUserAddressService.getUserAddress(orderForm.getUserAddressId(), SecurityConstants.INNER);
        // 判断远程调用是否出现异常
        if (R.FAIL == userAddressR.getCode()) {
            throw new ServiceException("远程调用微服务获取用户地址失败！");
        }
        UserAddress userAddress = userAddressR.getData();
        // 设置昵称
        orderInfo.setNickName(userAddress.getName());
        // 设置收货详细地址
        orderInfo.setReceiverAddress(userAddress.getAddress());
        // 设置收货的城市
        orderInfo.setReceiverCity(userAddress.getCityCode());
        // 设置地区
        orderInfo.setReceiverDistrict(userAddress.getDistrictCode());
        // 设置收货人姓名
        orderInfo.setReceiverName(userAddress.getName());
        // 设置收货人手机号
        orderInfo.setReceiverPhone(userAddress.getPhone());
        // 设置收货的省份
        orderInfo.setReceiverProvince(userAddress.getProvinceCode());
        // 设置标签名
        orderInfo.setReceiverTagName(userAddress.getTagName());

        // 插入订单表
        baseMapper.insert(orderInfo);

        // 通过主键回填获取orderId
        Long orderId = orderInfo.getId();

        // 3.批量插入订单项表
        orderItemList.forEach(orderItem -> {
            orderItem.setOrderId(orderId);
            orderItem.setRemark(orderInfo.getRemark());
        });
        orderItemService.saveBatch(orderItemList);

        // 4.插入订单日志
        OrderLog orderLog = new OrderLog();
        // 设置orderId
        orderLog.setOrderId(orderId);
        // 设置操作者
        orderLog.setCreateBy(SecurityContextHolder.getUserName());
        orderLog.setOperateUser(userAddress.getName());
        // 设置note
        orderLog.setNote("提交订单");
        // 设置订单状态
        orderLog.setProcessStatus(0);
        // 设置备注
        orderLog.setRemark(orderForm.getRemark());
        // 向数据库中插入数据
        orderLogMapper.insert(orderLog);

        // TODO:清空购物车
        remoteCartService.clearCheckedCartInfo(SecurityConstants.INNER);

        // TODO:发送一条延迟消息，30分钟订单未支付自动取消订单（测试阶段使用30s）
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, orderId);

        return orderId;
    }

    @Override
    public List<OrderInfo> myOrderInfoList(String orderStatus) {

        // 先根据Id查出该用户的所有订单，然后再根据订单状态查询
        Long userId = SecurityContextHolder.getUserId();
        // 创建List<OrderInfo>
        List<OrderInfo> orderInfoList = new ArrayList<>();

        // 创建查询条件
        LambdaQueryWrapper<OrderInfo> orderInfoQueryWrapper = new LambdaQueryWrapper<>();
        orderInfoQueryWrapper.eq(OrderInfo::getUserId, userId);
        // 判断orderStatus是否为空
        orderInfoQueryWrapper.eq(StringUtils.hasText(orderStatus), OrderInfo::getOrderStatus, orderStatus);
        orderInfoList = orderInfoMapper.selectList(orderInfoQueryWrapper);
        // 遍历查询订单项
        orderInfoList.forEach(orderInfo -> {
            // 根据订单Id查询
            Long orderInfoId = orderInfo.getId();
            List<OrderItem> orderItemList = new ArrayList<>();
            LambdaQueryWrapper<OrderItem> orderItemQueryWrapper = new LambdaQueryWrapper<>();
            orderItemQueryWrapper.eq(OrderItem::getOrderId, orderInfoId);
            orderItemList = orderItemMapper.selectList(orderItemQueryWrapper);
            orderInfo.setOrderItemList(orderItemList);
        });
        // 使用自定义的SQL语句查询数据
        //orderInfoList = orderInfoMapper.selectOrderInfoListByUserId(userId, orderStatus);

        return orderInfoList;
    }

    @Override
    public void cancelOrder(Long orderId, Integer flag) {
        // 更新点单状态
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);

        orderInfo.setCancelTime(new Date());
        // 设置取消的原因
        String cancelReason = flag == 1 ? "用户取消订单" : "系统取消订单";
        orderInfo.setCancelReason(cancelReason);

        // 设置订单状态,当订单状态为0时才设置，避免重复，使用条件的更新方法
        orderInfo.setOrderStatus(-1);
        int count = orderInfoMapper.update(orderInfo, new LambdaUpdateWrapper<OrderInfo>().eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getOrderStatus, 0));
        if (count == 1) { // 如果成功更新状态才将日志插入
            // 取消订单的同时，插入订单日志
            OrderLog orderLog = new OrderLog();
            orderLog.setOrderId(orderId);
            orderLog.setCreateBy(SecurityContextHolder.getUserName());
            orderLog.setOperateUser(flag == 1 ? "用户" : "系统");
            orderLog.setNote(cancelReason);
            orderLog.setProcessStatus(-1);
            orderLogMapper.insert(orderLog);
        }
    }
}
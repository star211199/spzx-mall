package com.spzx.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spzx.cart.api.RemoteCartService;
import com.spzx.cart.api.domain.CartInfo;
import com.spzx.common.core.constant.SecurityConstants;
import com.spzx.common.core.context.SecurityContextHolder;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.exception.ServiceException;
import com.spzx.common.core.utils.StringUtils;
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
import com.spzx.product.api.domain.vo.SkuLockVo;
import com.spzx.user.api.RemoteUserAddressService;
import com.spzx.user.domain.UserAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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
    private RedisTemplate<String, Object> redisTemplate;

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
        // 1.校验订单号，防止重复提交订单
        // 获取前端页面返回的订单号
        String tradeNo = orderForm.getTradeNo();
        // 后期Redis中的订单号
        String redisTradeNo = (String) redisTemplate.opsForValue().get("user:trade:" + tradeNo);
        // 使用Lua脚本保证Redis检查和删除操作的原子性，防止重复提交订单
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(
                org.springframework.data.redis.core.script.RedisScript.of(script, Long.class),
                Arrays.asList("user:trade:" + tradeNo),
                tradeNo
        );

        if (result == null || result == 0) {
            throw new ServiceException("下单超时或重复提交订单");
        }


        // 2.插入订单表
        OrderInfo orderInfo = new OrderInfo();
        // 先获取订单项列表（后续计算运费和总金额都需要用到）
        List<OrderItem> orderItemList = orderForm.getOrderItemList();
        // 使用后端计算的运费，不信任前端传递的运费
        BigDecimal freightFee = calculateFreight(orderItemList);
        orderInfo.setFeightFee(freightFee);
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

        // 搜集变动信息
        StringBuffer sb = new StringBuffer();
        // 遍历订单项计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem orderItem : orderItemList) {
            // 远程调用查询商品,获取sku信息（获取实时价格）
            R<ProductSku> productSkuR = remoteProductService.getProductSku(orderItem.getSkuId(), SecurityConstants.INNER);
            // 判断是否报错
            if (R.FAIL == productSkuR.getCode()) {
                throw new ServiceException("远程调用微服务查询sku信息失败!");
            }
            ProductSku productSku = productSkuR.getData();
            if (productSku == null) {
                throw new ServiceException("商品信息不存在，skuId: " + orderItem.getSkuId());
            }

            BigDecimal salePrice = productSku.getSalePrice();
            if (salePrice == null) {
                throw new ServiceException("商品价格信息异常，skuId: " + orderItem.getSkuId());
            }

            // 获取前端传递的旧价格（用于价格变动检测）
            BigDecimal skuPrice = orderItem.getSkuPrice();
            BigDecimal diff = skuPrice.subtract(salePrice);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("商品《%s》价格已从%.2f元调整为%.2f元，涨价%.2f元，请重新下单",
                        productSku.getSkuName(), skuPrice, salePrice, diff));
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                sb.append(String.format("商品《%s》价格已从%.2f元调整为%.2f元，降价%.2f元，请重新下单",
                        productSku.getSkuName(), skuPrice, salePrice, diff.abs()));
            }

            // 使用实时价格更新订单项价格
            orderItem.setSkuPrice(salePrice);

            // 获取商品的数量
            Integer skuNum = orderItem.getSkuNum();
            if (skuNum == null || skuNum <= 0) {
                throw new ServiceException("商品数量异常，skuId: " + orderItem.getSkuId());
            }

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
            // 计算金额小计（使用实时价格）
            BigDecimal amount = salePrice.multiply(new BigDecimal(skuNum));
            // 累加总金额
            totalAmount = totalAmount.add(amount);
        }
        // 订单总金额 = 商品总金额 + 运费
        BigDecimal orderTotalAmount = totalAmount.add(freightFee);
        orderInfo.setTotalAmount(orderTotalAmount);
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
        // 先查询订单当前状态
        OrderInfo existingOrder = orderInfoMapper.selectById(orderId);
        if (existingOrder == null) {
            throw new ServiceException("订单不存在");
        }

        // 只有待支付状态（orderStatus=0）的订单才能取消
        if (existingOrder.getOrderStatus() != 0) {
            String statusMsg = existingOrder.getOrderStatus() == -1 ? "已取消"
                    : existingOrder.getOrderStatus() == 1 ? "已支付"
                    : existingOrder.getOrderStatus() == 2 ? "已完成"
                    : "未知状态";
            throw new ServiceException("订单状态已变更（" + statusMsg + "），无法取消");
        }

        // 设置取消的原因
        String cancelReason = flag == 1 ? "用户取消订单" : "系统取消订单";

        // 使用条件的更新方法，只更新状态为0的订单（乐观锁机制）
        int count = orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getOrderStatus, 0) // 确保订单状态仍为待支付
                .set(OrderInfo::getOrderStatus, -1)
                .set(OrderInfo::getCancelTime, new Date())
                .set(OrderInfo::getCancelReason, cancelReason)
        );

        if (count == 0) {
            // 说明订单状态已经被其他操作改变，抛出异常
            throw new ServiceException("订单状态已变更，取消失败");
        }

        // 取消订单成功，插入订单日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderId);
        orderLog.setCreateBy(SecurityContextHolder.getUserName());
        orderLog.setOperateUser(flag == 1 ? "用户" : "系统");
        orderLog.setNote(cancelReason);
        orderLog.setProcessStatus(-1);
        orderLogMapper.insert(orderLog);

        // 只有成功取消订单后才还原库存和销量
        // 根据orderId获取orderItem信息
        List<OrderItem> orderItemList = orderItemService.list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (!CollectionUtils.isEmpty(orderItemList)) {
            // 将orderItemList --> skuLockVoList
            List<SkuLockVo> skuLockVoList = orderItemList.stream().map(orderItem -> {
                SkuLockVo skuLockVo = new SkuLockVo();
                skuLockVo.setSkuId(orderItem.getSkuId());
                skuLockVo.setSkuNum(orderItem.getSkuNum());
                return skuLockVo;
            }).collect(Collectors.toList());
            // 远程调用微服务处理
            R<String> stringR = remoteProductService.addStockAndSubSaleNum(skuLockVoList, SecurityConstants.INNER);
            if (R.FAIL == stringR.getCode()) {
                // 库存还原失败，记录日志但不抛异常（订单已取消）
                log.error("订单取消成功，但库存还原失败，订单号：{}", orderId);
            }
        }
    }

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        // 先通过订单号查询的订单的信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        // 同意订单Id查询对应的订单项
        List<OrderItem> orderItemList = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderInfo.getId()));
        orderInfo.setOrderItemList(orderItemList);

        return orderInfo;
    }

    /**
     * 计算运费
     * 基础运费 + 超重运费（可以接入第三方物流API或配置表）
     *
     * @param orderItemList 订单项列表
     * @return 运费金额
     */
    private BigDecimal calculateFreight(List<OrderItem> orderItemList) {
        if (CollectionUtils.isEmpty(orderItemList)) {
            return BigDecimal.ZERO;
        }

        // 基础运费
        BigDecimal baseFreight = new BigDecimal("10.00");

        // 计算商品总数量
        int totalNum = orderItemList.stream()
                .mapToInt(OrderItem::getSkuNum)
                .sum();

        // 如果订单金额超过99元免运费
        BigDecimal totalAmount = orderItemList.stream()
                .map(item -> item.getSkuPrice().multiply(new BigDecimal(item.getSkuNum())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(new BigDecimal("99.00")) >= 0) {
            return BigDecimal.ZERO;
        }

        // 超过5件额外收费
        if (totalNum > 5) {
            int extraNum = totalNum - 5;
            BigDecimal extraFreight = new BigDecimal("2.00").multiply(new BigDecimal(extraNum));
            return baseFreight.add(extraFreight);
        }

        return baseFreight;
    }
}
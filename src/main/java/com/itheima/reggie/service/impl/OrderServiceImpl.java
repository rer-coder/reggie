package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.OrderDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.service.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Transactional
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {
    @Autowired
    private UserService userService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private OrderDetailService orderDetailService;
    /**
     * 用户下单
     * @param orders
     */
    @Override
    public void submit(Orders orders) {
        //获取当前用户id
        Long currentId = BaseContext.getCurrentId();

        //查询购物车数据
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, currentId);
        List<ShoppingCart> list = shoppingCartService.list(lambdaQueryWrapper);
        if (list == null || list.size() == 0) {
            throw new CustomException("购物车为空，不能下单");
        }

        // 刚方法返回的值是分布式系统唯一的标识符,也就是分布式ID
        long orderId = IdWorker.getId();  //订单id

        //遍历购物车，计算总金额，以及构建详细订单数据
        AtomicInteger amount = new AtomicInteger(0);
        List<OrderDetail> orderDetails = list.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        //根据currentId获取用户
        User user = userService.getById(currentId);

        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("用户地址信息有误，不能下单");
        }

        //向订单表插入数据
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));  //总金额
        orders.setUserId(currentId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName()); //登录用户名字
        orders.setConsignee(addressBook.getConsignee()); //收货人名字
        orders.setPhone(addressBook.getPhone());  //收货人电话
        orders.setAddress((addressBook.getProvinceName() == "" ? "" : addressBook.getProvinceName())
        + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
        + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
        + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        this.save(orders);

        // 向订单明细表插入数据
        orderDetailService.saveBatch(orderDetails);

        // 去清空购物车
        shoppingCartService.remove(lambdaQueryWrapper);
    }

    /**
     * 获取用户的订单分页
     * @param page 页码号
     * @param pageSize 每页的大小
     * @return
     */
    @Override
    public IPage<OrderDto> getUserPage(int page, int pageSize) {
        // 构造分页对象
        Page<Orders> ordersPage = new Page<Orders>(page, pageSize);
        // 没有传入参数，是因为有默认的参数，默认为1页10条。
        // 因为创建的orderDtoPage对象最终要返回，所以就新建一个，复制返回呗
        Page<OrderDto> orderDtoPage = new Page<>();
        // 构造查询条件
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getOrderTime);
        // 查询基本订单数据
        this.page(ordersPage, queryWrapper);
        // 拷贝对象，将ordersPage中的数据拷贝到dtoPage中，除去records属性,records属性需要单独处理
        BeanUtils.copyProperties(ordersPage, orderDtoPage, "records");
        // records是Page对象的一个属性，用于存放分页查询的结果，因为orderPage的records很多数据比dtoPage的少，需要单独处理
        List<Orders> ordersPageRecords = ordersPage.getRecords();
        List<OrderDto> orderDtosPageRecords = ordersPageRecords.stream().map(item -> {
            // 1.先创建OrdersDto对象
            OrderDto orderDto = new OrderDto();
            // 2.将Orders对象中的基本数据拷贝到OrdersDto对象中
            BeanUtils.copyProperties(item, orderDto);
            // 3.查询订单商品数据
            LambdaQueryWrapper<OrderDetail> orderDetailQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailQueryWrapper.eq(OrderDetail::getOrderId, item.getId());
            List<OrderDetail> orderDetails = orderDetailService.list(orderDetailQueryWrapper);
            // 4.将订单商品数据设置到OrdersDto对象中
            orderDto.setOrderDetails(orderDetails);
            // 5.将其它数据设置到OrdersDto对象中
            LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.eq(User::getId, item.getUserId());
            User user = userService.getOne(userLambdaQueryWrapper);
            if(user != null){
                orderDto.setUserName(user.getName());
            }
            orderDto.setAmount(item.getAmount());
            orderDto.setConsignee(item.getConsignee());
            orderDto.setAddress(item.getAddress());
            orderDto.setConsignee(item.getConsignee());

            return orderDto;
        }).collect(Collectors.toList());
        // 将转换后的数据设置到dtoPage中
        orderDtoPage.setRecords(orderDtosPageRecords);
        return orderDtoPage;
    }

    /**
     * 获取管理员订单详情
     * @param page 页码号
     * @param pageSize 每页的数量
     * @param number 订单号
     * @param beginTime 开始的时间
     * @param endTime 结束的时间
     * @return
     */
    @Override
    public IPage<OrderDto> getPages(int page, int pageSize, String number, Date beginTime, Date endTime) {
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        Page<OrderDto> ordersDtoPage = new Page<>();
        LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
        ordersQueryWrapper.orderByDesc(Orders::getOrderTime);
        ordersQueryWrapper.like(StringUtils.isNotEmpty(number), Orders::getNumber, number);
        ordersQueryWrapper.between((beginTime != null && endTime != null), Orders::getOrderTime, beginTime, endTime);
        this.page(ordersPage, ordersQueryWrapper);
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");
        List<Orders> ordersPageRecords = ordersPage.getRecords();
        List<OrderDto> orderDtoPageRecords = ordersPageRecords.stream().map(item -> {
           OrderDto orderDto = new OrderDto();
           BeanUtils.copyProperties(item, orderDto);
           LambdaQueryWrapper<OrderDetail> orderDetailQueryWrapper = new LambdaQueryWrapper<>();
           orderDetailQueryWrapper.eq(OrderDetail::getOrderId, item.getId());
           List<OrderDetail> orderDetails = orderDetailService.list(orderDetailQueryWrapper);
           orderDto.setOrderDetails(orderDetails);
           LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
           userLambdaQueryWrapper.eq(User::getId, item.getId());
           User user = userService.getOne(userLambdaQueryWrapper);
           if(user != null){
               orderDto.setUserName(user.getName());
           }
           orderDto.setAmount(item.getAmount());
           orderDto.setConsignee(item.getConsignee());
           orderDto.setAddress(item.getAddress());
           orderDto.setConsignee(item.getConsignee());

           return orderDto;

        }).collect(Collectors.toList());
        ordersDtoPage.setRecords(orderDtoPageRecords);
        return ordersDtoPage;
    }
}

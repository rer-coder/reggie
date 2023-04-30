package com.itheima.reggie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.OrderDto;
import com.itheima.reggie.entity.Orders;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public interface OrderService extends IService<Orders> {
    /**
     * 用户下单
     * @param orders
     */
    public void submit(Orders orders);

    /**
     * 获取用户订单列表，以分页的形式展示
     * @param page 页码号
     * @param pageSize 每页的大小
     * @return
     */
    public IPage<OrderDto> getUserPage(int page, int pageSize);

    /**
     *  获取管理员订单的详情
     * @param page 页码号
     * @param pageSize 每页的数量
     * @param number 订单号
     * @param beginTime 开始的时间
     * @param endTime 结束的时间
     * @return
     */
    public IPage<OrderDto> getPages(int page, int pageSize, String number,
                                   @DateTimeFormat(pattern = "yyyy-mm-dd HH:mm:ss") Date beginTime,
                                   @DateTimeFormat(pattern = "yyyy-mm-dd HH:mm:ss") Date endTime);


}

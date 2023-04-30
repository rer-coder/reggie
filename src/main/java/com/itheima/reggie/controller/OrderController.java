package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrderDto;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 处理用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        log.info("订单数据：{}", orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 用于前端查询订单详情
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<IPage<OrderDto>> getUserPages(int page, int pageSize) {
        // 获取用户订单分页
        IPage<OrderDto> userPage = orderService.getUserPage(page, pageSize);
        return R.success(userPage);
    }

    /**
     * 用于后端的，分页查询总订单，如果有订单号或者日期，则同时考虑订单号和日期
     * @param page 页号
     * @param pageSize 每页数量
     * @param number 订单号
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return
     */
    @GetMapping("/page")
    public R<IPage<OrderDto>> getPages(
            int page, int pageSize, String number,
            @DateTimeFormat(pattern = "yyyy-mm-dd HH:mm:ss") Date beginTime,
            @DateTimeFormat(pattern = "yyyy-mm-dd HH:mm:ss") Date endTime
            ){
        log.info(
                "订单分页查询：page={}，pageSize={}，number={},beginTime={},endTime={}",
                page,
                pageSize,
                number,
                beginTime,
                endTime);
        // 根据以上信息进行分页查询。
        // 创建分页对象
        IPage<OrderDto> pageInfo = orderService.getPages(page, pageSize, number, beginTime, endTime);
        return R.success(pageInfo);
    }

}

package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author hpc
 * @create 2023/4/5 16:58
 */
@RestController
@Slf4j
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("购物车数据：{}", shoppingCart);
        //设置用户id，指定当前购物车是属于那一个用户
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);
        //构造查询条件
        Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, currentId); //第一个条件
        if (dishId != null) {
            //要添加到购物车的是菜品
            lambdaQueryWrapper.eq(ShoppingCart::getDishId, dishId); //第二个条件
        } else {
            //要添加到购物车的是套餐
            lambdaQueryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        //查询当前菜品或者套餐是否已经在购物车中
        ShoppingCart one = shoppingCartService.getOne(lambdaQueryWrapper);
        // 如果已经存在则在原来数量基础上加一
        if (one != null) {
            Integer number = one.getNumber();
            one.setNumber(number+1);
            shoppingCartService.updateById(one);
        } else {
            // 如果不存在，则添加到购物车，数量为1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            one = shoppingCart; //方便返回
        }
        return R.success(one);
    }

    /**
     * 减少购物车中的菜品或套餐
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        // 获取当前登录用户id
        Long currentId = BaseContext.getCurrentId();
        // 创建查询条件
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, currentId);
        // 根据菜品或套餐id获取物品
        if (shoppingCart.getDishId() != null) {
            //是菜品,使用菜品查询条件
            lambdaQueryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else {
            // 是套餐，使用套餐查询条件
            lambdaQueryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        // 查询
        ShoppingCart one = shoppingCartService.getOne(lambdaQueryWrapper);
        // 获取所查询到的物品的数量
        Integer number = one.getNumber();
        //number减一
        number -= 1;
        one.setNumber(number);
        // 如果大于1更新数据
        if (number >= 1) {
            shoppingCartService.updateById(one);
        } else {
            // 如果小于等于1，从数据库中删除
            shoppingCartService.removeById(one);
        }
        return R.success(one);
    }
    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        log.info("查看购物车");
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        lambdaQueryWrapper.orderByAsc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(lambdaQueryWrapper);
        return R.success(list);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean() {
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        //注意此处不能根据removeById删除，因为这个id不是实体类ShoppingCart的id。
        shoppingCartService.remove(lambdaQueryWrapper);
        return R.success("清空购物车成功");
    }
}

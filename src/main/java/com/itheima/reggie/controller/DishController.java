package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author hpc
 * @create 2023/3/12 10:34
 * 菜品管理
 */
@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息的分页
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //分页构造器
        Page<Dish> pageinfo = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        lambdaQueryWrapper.like(name != null, Dish::getName, name);
        //添加排序条件
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        dishService.page(pageinfo, lambdaQueryWrapper);

        Page<DishDto> dishDtoPage = new Page<>();
        //对象拷贝
        BeanUtils.copyProperties(pageinfo, dishDtoPage, "records");
        List<Dish> records =  pageinfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
           Long categoryId =  item.getCategoryId(); //菜品分类id
           Category category = categoryService.getById(categoryId);
           if (category != null) {
               String categoryName = category.getName();
               dishDto.setCategoryName(categoryName);
           } else {
               dishDto.setCategoryName("无分类");
           }
           return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
          // 对dish下面的所有分类都清除
//        Set keys = redisTemplate.keys("dish_");
//        redisTemplate.delete(keys);

        // 清理某个分类下面的菜品数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        dishService.updateWithFlavor(dishDto);
        return R.success("修改菜品成功");
    }

    //删除菜品
    @DeleteMapping
    public R<String> delete(String ids) {

        //这部分代码自己写的，太孬了！参考更改更改菜品状态写法。
        //将接收到的String类型的ids转换为List
        String[] idArray = StringUtils.split(ids, ",");
        List<Long> idList = Arrays.stream(idArray).map(Long::parseLong).collect(Collectors.toList());
        //在service写删除逻辑
        List<DishDto> dishDtoList = dishService.deleteWithFlavors(idList);
        dishDtoList.stream().map((item) -> {
            String key = "dish_" + item.getCategoryId() + "_1";
            redisTemplate.delete(key);
            return item;
        }).collect(Collectors.toList());
        return R.success("删除菜品成功");
    }

    //更改菜品状态
    @PostMapping("/status/{status}")
    public R<String> updateState(@PathVariable int status, String[] ids) {
        for (String id : ids) {
            Dish dish = dishService.getById(id);
            dish.setStatus(status);
            dishService.updateById(dish);
            String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
            redisTemplate.delete(key);
        }
        return R.success("状态修改成功");
    }

    //根据categoryId查找菜品
    /*@GetMapping("/list")
    public R<List<Dish>> list(Dish dish) {
        //构建查询条件对象
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        //只查在售的菜品
        lambdaQueryWrapper.eq(Dish::getStatus, 1);
        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> dishList = dishService.list(lambdaQueryWrapper);
        return R.success(dishList);
    }*/
    //根据categoryId查找菜品
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {

        List<DishDto> dishDtoList = null;
        // 动态构造key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        // 如果有，则直接返回
        if(dishDtoList != null){
            return R.success(dishDtoList);
        }

        //构建查询条件对象
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        //只查在售的菜品
        lambdaQueryWrapper.eq(Dish::getStatus, 1);
        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> dishList = dishService.list(lambdaQueryWrapper);
        dishDtoList = dishList.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long dishId = item.getId(); //菜品id
            LambdaQueryWrapper<DishFlavor> qw = new LambdaQueryWrapper<>();
            qw.eq(DishFlavor::getDishId, dishId);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(qw);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());
        // 从数据库中查出来以后，将菜品重新保存到Redis中，使得下次好取
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);
        return R.success(dishDtoList);
    }
}

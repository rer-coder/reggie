package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hpc
 * @create 2023/3/7 21:58
 */
@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存口味数据
     *
     * @param dishDto
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息
        this.save(dishDto);
        //菜品id，继承自dish
        Long dishId = dishDto.getId();
        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());
        //保存菜品口味表
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据菜品id查询菜品信息以及对应的口味信息
     *
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品信息
        Dish dish = this.getById(id);
        //根据dish的id查询口味
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper);
        //设置DishDto
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    @Override
    public void updateWithFlavor(DishDto dishDto) {
        this.updateById(dishDto);
        //清理口味数据
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(lambdaQueryWrapper);
        //添加新的口味数据
        List<DishFlavor> flavors = dishDto.getFlavors();
        //注意flavors中的DishFlavor对象是没有dish_id的，所以保存到数据库时我们要将dish_id封装进去
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }

    //删除，批量删除菜品
    @Override
    @Transactional
    public List<DishDto> deleteWithFlavors(List<Long> idList) {

        List<DishDto> dishDtoList = idList.stream().map((ids) -> {
            DishDto dishDto = new DishDto();

            Dish byID = this.getById(ids);

            this.removeById(byID);

            BeanUtils.copyProperties(byID, dishDto);

            //清理当前菜品对应口味数据---dish_flavor表的delete操作
            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DishFlavor::getDishId, ids);

            dishFlavorService.remove(queryWrapper);

            return dishDto;
        }).collect(Collectors.toList());

        return dishDtoList;
    }
}


//    /**
//     * 删除或批量删除菜品
//     * @param list
//     */
//    @Override
//    @Transactional
//    public List<DishDto> deleteWithFlavors(List<Long> list) {
//
//        List<DishDto> dishDtoList= list.stream().map((ids) -> {
//            DishDto dishDto = new DishDto();
//
//            Dish byId = this.getById(ids);
//
//            this.removeById(ids);
//
//            BeanUtils.copyProperties(byId,dishDto);
//
//            //清理当前菜品对应口味数据---dish_flavor表的delete操作
//            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
//            queryWrapper.eq(DishFlavor::getDishId,ids);
//
//            dishFlavorService.remove(queryWrapper);
//
//            return dishDto;
//        }).collect(Collectors.toList());
//
//        return dishDtoList;
//    }
//}

package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

import java.util.List;

/**
 * @author hpc
 * @create 2023/3/7 21:56
 */
public interface DishService extends IService<Dish> {
    //新增菜品，同时插入菜品对应的口味数据，需要操作2张表
    void saveWithFlavor(DishDto dishDto);
    //查询菜品的同时查询口味
    DishDto getByIdWithFlavor(Long id);

    //更新菜品信息同时更新口味信息
    void updateWithFlavor(DishDto dishDto);

    List<DishDto> deleteWithFlavors(List<Long> idList);
}

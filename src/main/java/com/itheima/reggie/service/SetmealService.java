package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

import java.util.List;

/**
 * @author hpc
 * @create 2023/3/7 21:57
 */
public interface SetmealService extends IService<Setmeal> {
    public void saveWithDish(SetmealDto setmealDto);

    Page<SetmealDto> pageWithCategoryName(int page, int pageSize, String name);

    void removeWithDish(List<Long> ids);

    SetmealDto getSetmealWithDish(Long id);

    void updateWithDish(SetmealDto setmealDto);
}

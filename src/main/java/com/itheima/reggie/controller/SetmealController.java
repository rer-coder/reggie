package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hpc
 * @create 2023/3/13 16:28
 * 套餐管理
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;

    //新增套餐
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息{}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("成功");
    }

    //套餐分页查询
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        Page<SetmealDto> pageinfo = setmealService.pageWithCategoryName(page, pageSize, name);
        return R.success(pageinfo);
    }

    //删除套餐
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {  //用String[] 或Long[]接收不用加注解，使用List<Long>需要加注解
        //删除套餐
        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功");
    }

    //根据id查询
    @GetMapping("{id}")
    public R<SetmealDto> get(@PathVariable Long id) {
        SetmealDto setmealDto = setmealService.getSetmealWithDish(id);
        return R.success(setmealDto);
    }

    //修改套餐
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息{}", setmealDto);
        setmealService.updateWithDish(setmealDto);
        return R.success("成功");
    }

    //套餐停售起售
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable int status, @RequestParam List<Long> ids) {
        //根据idList查询setmealList
        List<Setmeal> setmealList = setmealService.listByIds(ids);
        //更改status
        setmealList.stream().map((item) -> {
            item.setStatus(status);
            return item;
        }).collect(Collectors.toList());
        setmealService.updateBatchById(setmealList);
        return R.success("更改状态成功");
    }

    /**
     * 根据套餐分类id获取所有套餐
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        lambdaQueryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(lambdaQueryWrapper);
        return R.success(list);
    }
}

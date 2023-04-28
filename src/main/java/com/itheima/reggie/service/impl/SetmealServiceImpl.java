package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hpc
 * @create 2023/3/7 21:59
 */
@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;
    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息
        this.save(setmealDto);
        //保存套餐和菜品的关联信息
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 套餐的分页查询，同时查询套餐分类的名称
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @Override
    public Page<SetmealDto> pageWithCategoryName(int page, int pageSize, String name) {
        //构建setmeal的Page对象
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        //构建查询条件
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(name != null, Setmeal::getName, name);
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //Setmeal表查询
        this.page(pageInfo, lambdaQueryWrapper);
        //构建setmealdto的page对象
        Page<SetmealDto> setmealDtoPage = new Page<>();
        //Page<Setmeal>和Page<SetmealDto>中的records属性泛型不一样，所以不用拷贝
        BeanUtils.copyProperties(pageInfo, setmealDtoPage, "records");
        //自己设置setmealdtopage中的records
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> setmealDtoList = records.stream().map((item) -> {
            // 创建SetmealDto对象
            SetmealDto setmealDto = new SetmealDto();
            //将item中的所有属性值拷贝给setmealDto
            BeanUtils.copyProperties(item, setmealDto);
            //得到setmeal中的category_id
            Long categoryId = item.getCategoryId();
            //根据categoryId在category表中查询category实体，进而查出categoryName
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPage.setRecords(setmealDtoList);
        return setmealDtoPage;
    }

    /**
     * 删除套餐同时删除关联的菜品setmealdish
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //查询要删除的套餐的状态，如果有在售状态则抛出异常
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Setmeal::getId, ids);
        lambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        int count = this.count(lambdaQueryWrapper);
        if (count > 0) {
            throw new CustomException("套餐在售买中，不能删除");
        }
        //接下来正式删除，删除套餐之后删除关联的菜品
        this.removeByIds(ids);
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(setmealDishLambdaQueryWrapper);
    }

    /**
     * 根据套餐id查询套餐并查询该套餐的菜品
     * @param id
     * @return
     */
    @Override
    public SetmealDto getSetmealWithDish(Long id) {
        //查询套餐
        Setmeal setmeal = this.getById(id);
        //根据setmeal的id查询菜品列表
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishList = setmealDishService.list(lambdaQueryWrapper);
        //封装到setmealdto
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);
        setmealDto.setSetmealDishes(setmealDishList);
        return setmealDto;
    }

    /**
     * 更新套餐。同时更新套餐表和套餐所附带的菜品表
     * @param setmealDto
     */
    @Override
    @Transactional
    public void updateWithDish(SetmealDto setmealDto) {
        //对于套餐表，直接更新就行了
        this.updateById(setmealDto);
        //但是对于套餐所关联的菜品是List<SetmealDish>，我们需要删除所有，重新添加
        setmealDishService.remove(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmealDto.getId()));
        List<SetmealDish> setmealDishList = setmealDto.getSetmealDishes();
        //注意，setmealdish实体的setmealid是为null的，但是setmealdish表中是有setmealid的字段的，需要加进去
        setmealDishList.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDishList);
    }
}

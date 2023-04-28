package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Category;

/**
 * @author hpc
 * @create 2023/3/7 19:52
 */
public interface CategoryService extends IService<Category> {
    public void remove(Long id);
}

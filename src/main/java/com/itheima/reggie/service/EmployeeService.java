package com.itheima.reggie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Employee;

/**
 * @author hpc
 * @create 2023/2/28 8:40
 */
public interface EmployeeService extends IService<Employee> {
    IPage<Employee> getPage(int page, int pageSize, String name);
}

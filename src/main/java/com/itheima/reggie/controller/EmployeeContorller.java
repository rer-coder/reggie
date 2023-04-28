package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * @author hpc
 * @create 2023/2/28 8:45
 */
@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeContorller {
    @Autowired
    EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        //将密码进行md5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        //查询数据库(条件查询)
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);
        //如果没有查到返回登录失败结果
        if (emp == null) {
            return R.error("登录失败");
        }
        //密码比对
        if (!emp.getPassword().equals(password)) {
            return R.error("密码错误");
        };
        //查看员工状态是否被禁用
        if (emp.getStatus() == 0) {
            return R.error("账号已禁用");
        }
        //登陆成功将员工的数据保存在Session中，员工数据少，没必要保存在Redis中
        request.getSession().setAttribute("employee", emp.getId());

        return R.success(emp);
    }

    /**
     * 员工退出的方法
     */
    @PostMapping("logout")
    public R<String> logout(HttpServletRequest request) {
        //清理Session中保存的当前登录员工的id
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    /**
     * 新增员工
     */
    @PostMapping
    public R<String> save(@RequestBody Employee employee, HttpServletRequest request) {
        //设置初始密码123456,需要md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
        //获得当前登录用户的id
        Long empid = (Long) request.getSession().getAttribute("employee");
//        employee.setCreateUser(empid);
//        employee.setUpdateUser(empid);
//        log.info("新增员工信息{}", employee.toString());
        employeeService.save(employee);
        return R.success("新增员工成功");
    }

    //分页查询
    @GetMapping("/page")
    public R<IPage> page(int page, int pageSize, String name) {
//        log.info("page:{}, pagesize:{}, name:{}", page, pageSize, name);
        IPage pageInfo = employeeService.getPage(page, pageSize, name);
        return R.success(pageInfo);
    }

    //根据id修改员工信息
    @PutMapping
    public R<String> update(@RequestBody Employee employee, HttpServletRequest request) {
//        log.info("员工信息{}", employee.toString());
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser((Long) request.getSession().getAttribute("employee"));
        long id = Thread.currentThread().getId();
        log.info("线程id为{}", id);
        employeeService.updateById(employee);
        return R.success("员工信息修改成功");
    }

    //根据id获取用户
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id) {
        log.info("根据id查询员工信息");
        Employee employee = employeeService.getById(id);
        if (employee != null) {
            return R.success(employee);
        } else {
         return R.error("没有查询到员工信息");
        }
    }
}

package com.itheima.reggie.common;

/**
 * @author hpc
 * @create 2023/3/3 16:55
 * 基于ThreadLocal封装的工具类，用于保存和获取当前登录用户的id
 * 同一个线程之内，ThreadLocal对象可以保存一些变量
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }
    public static Long getCurrentId() {
        return threadLocal.get();
    }
}

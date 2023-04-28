package com.itheima.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author hpc
 * @create 2023/3/25 20:35
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author hpc
 * @create 2023/2/28 14:40
 */
//检查用户是否登录
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        //获取本次请求的URI
        String requestURI = request.getRequestURI();
        log.info("拦截到请求{}", requestURI);
        //定义一些不需要处理的请求，直接放行
        String[] urls = new String[] {
          "/employee/login",
          "/employee/logout",
          "/backend/**",
          "/front/**",
          "/common/**",
          "/user/sendMsg",  //移动端发送短信
          "/user/login"  //移动端登录
        };
        //判断本次请求是否不需要处理
        boolean check = check(urls, requestURI);
        if (check) { //不需要处理直接放行
            log.info("本次请求{}不需要处理", requestURI);
            filterChain.doFilter(request, response); //进入下一个拦截器或Servlet
            return;
        }
        //对于需要处理的请求，查看登录状态，如果登录则放行
        if (request.getSession().getAttribute("employee") != null) {
            Long empId = (Long)request.getSession().getAttribute("employee");
            log.info("用户已经登录，用户id为{}", empId);
            long id = Thread.currentThread().getId();
            log.info("线程id为{}", id);
            BaseContext.setCurrentId(empId);  //设置ThreadLocal一定要在doFilter之前
            filterChain.doFilter(request, response);
            return;
        }
        //查看前台登录状态，如果登录则放行
        if (redisTemplate.opsForValue().get("user") != null) {
            Long userId = (Long)redisTemplate.opsForValue().get("user");
            BaseContext.setCurrentId(userId);  //设置ThreadLocal一定要在doFilter之前
            filterChain.doFilter(request, response);
            return;
        }
        //如果没有登录返回未登录结果,通过输出流向客户端页面响应数据
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
        //log.info("拦截到的请求: {}", request.getRequestURL());
    }

    //检查本次匹配是否需要放行
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) return true;
        }
        return false;
    }
}

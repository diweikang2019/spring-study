package com.kang.springstudy.mini.demo.controller;

import com.kang.springstudy.mini.demo.service.DemoService;
import com.kang.springstudy.mini.framework.annotation.MyAutowired;
import com.kang.springstudy.mini.framework.annotation.MyController;
import com.kang.springstudy.mini.framework.annotation.MyRequestMapping;
import com.kang.springstudy.mini.framework.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author weikang.di
 * @date 2023/5/24 0:24
 */
@MyRequestMapping("demo")
@MyController
public class DemoController {

    @MyAutowired
    private DemoService demoService;

    @MyRequestMapping("query")
    public void query(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name) throws IOException {
        String result = demoService.query(name);

        response.getWriter().write(result);
    }
}

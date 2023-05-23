package com.kang.springstudy.mini.demo.service.impl;

import com.kang.springstudy.mini.demo.service.DemoService;
import com.kang.springstudy.mini.framework.annotation.MyService;

/**
 * @author weikang.di
 * @date 2023/5/24 0:22
 */
@MyService
public class DemoServiceImpl implements DemoService {

    @Override
    public String query(String name) {
        return "My name is " + name + ",from service.";
    }
}

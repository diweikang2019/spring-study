package com.kang.springstudy.mini.framework;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author weikang.di
 * @date 2023/5/24 0:11
 */
public class MyDispatcherServlet extends HttpServlet {

    // 保存读取的配置文件内容
    private Properties contextConfig = new Properties();

    // 缓存从包路径下扫描的全类名
    private List<String> classNames = new ArrayList<>();

    // 保存所有扫描的类的实例
    private Map<String, Object> ioc = new HashMap<>();

    // 保存Controller中URL和Method的对应关系
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 6.根据URL委派给具体的方法
            this.doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception, Detail: " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {

    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        // 1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2.扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        // ========== IoC功能 ==========
        // 3、初始化IoC容器，将扫描到的类进行实例化，缓存到IoC容器中
        doInstance();

        // ========== DI功能 ==========
        // 4.依赖注入
        doAutowired();

        // ========== MVC功能 ==========
        // 5.初始化HandlerMapping
        doInitHandlerMapping();
    }

    private void doLoadConfig(String contextConfigLocation) {
    }

    private void doScanner(String scanPackage) {
    }

    private void doInstance() {
    }

    private void doAutowired() {
    }

    private void doInitHandlerMapping() {
    }
}

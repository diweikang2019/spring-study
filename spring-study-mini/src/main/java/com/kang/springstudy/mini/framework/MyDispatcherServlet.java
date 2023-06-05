package com.kang.springstudy.mini.framework;

import com.kang.springstudy.mini.framework.annotation.MyAutowired;
import com.kang.springstudy.mini.framework.annotation.MyController;
import com.kang.springstudy.mini.framework.annotation.MyRequestMapping;
import com.kang.springstudy.mini.framework.annotation.MyRequestParam;
import com.kang.springstudy.mini.framework.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * @author weikang.di
 * @date 2023/5/24 0:11
 */
public class MyDispatcherServlet extends HttpServlet {

    /**
     * 保存读取的配置文件内容
     */
    private Properties contextConfig = new Properties();

    /**
     * 缓存从包路径下扫描的全类名
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * 保存所有扫描的类的实例
     */
    private Map<String, Object> ioc = new HashMap<>();

    /**
     * 保存Controller中URL和Method的对应关系
     */
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 6、根据URL委派给具体的方法
            this.doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception, Detail: " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!!!");
            return;
        }

        Method method = this.handlerMapping.get(url);

        Object[] paramValues = this.getParams(req, resp, method);

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        // 3、组成动态实际参数列表，传给反射调用
        method.invoke(ioc.get(beanName), paramValues);
    }

    /**
     * 获取参数
     *
     * @param req
     * @param resp
     * @param method
     * @return
     */
    private Object[] getParams(HttpServletRequest req, HttpServletResponse resp, Method method) {
        // 1、先把形参的位置和参数名字建立映射关系，并且缓存下来
        Map<String, Integer> paramIndexMapping = new HashMap<>(8);

        Annotation[][] pa = method.getParameterAnnotations();
        for (int i = 0; i < pa.length; i++) {
            for (Annotation a : pa[i]) {
                if (a instanceof MyRequestParam) {
                    String paramName = ((MyRequestParam) a).value();
                    if (!"".equals(paramName.trim())) {
                        paramIndexMapping.put(paramName, i);
                    }
                }
            }
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> type = paramTypes[i];
            if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                paramIndexMapping.put(type.getName(), i);
            }
        }

        // 2、根据参数位置匹配参数名字，从url中取到参数名字对应的值
        Object[] paramValues = new Object[paramTypes.length];

        // http://localhost/demo/query?name=Tom&name=Tomcat&name=Mic
        Map<String, String[]> params = req.getParameterMap();
        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue())
                    .replaceAll("\\[|\\]", "")
                    .replaceAll("\\s", "");

            if (!paramIndexMapping.containsKey(param.getKey())) {
                continue;
            }

            int index = paramIndexMapping.get(param.getKey());

            // 涉及到类型强制转换
            paramValues[index] = value;
        }

        if (paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int index = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[index] = req;
        }

        if (paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int index = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[index] = resp;
        }

        return paramValues;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        // 1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2、扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        // ========== IoC功能 ==========
        // 3、初始化IoC容器，将扫描到的类进行实例化，缓存到IoC容器中
        doInstance();

        // ========== DI功能 ==========
        // 4、依赖注入
        doAutowired();

        // ========== MVC功能 ==========
        // 5、初始化HandlerMapping
        doInitHandlerMapping();
    }

    /**
     * 加载配置文件
     *
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (Objects.nonNull(inputStream)) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.getStackTrace();
                }
            }
        }
    }

    /**
     * 扫描相关的类
     *
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        // 转化为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());

        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }

                String className = scanPackage + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    /**
     * 实例化加了注解的类
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(MyController.class)) {
                    // 首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    // 1、优先使用别名（自定义命名）
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String beanName = myService.value();

                    // 2、默认类名首字母小写
                    if ("".equals(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);

                    // 3、如果是接口，只能初始化的它的实现类
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The " + i.getName() + " is exists, please use alies!!");
                        }
                        // 直接把接口的类型当成key
                        ioc.put(i.getName(), instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 依赖注入
     */
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 获取实例的所有字段，忽略字段的修饰符，不管你是 private / protected / public / default
            for (Field field : entry.getValue().getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }

                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                String beanName = myAutowired.value();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }

                // 代码在反射面前，那就是裸奔
                // 强制访问，强吻
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.getStackTrace();
                }
            }
        }
    }

    /**
     * 初始化HandlerMapping
     * 初始化url和Method的映射关系
     */
    private void doInitHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }

            String baseUrl = "/";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl += myRequestMapping.value();
            }

            // 只迭代public方法
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }

                MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = (baseUrl + "/" + myRequestMapping.value()).replaceAll("/+", "/");

                handlerMapping.put(url, method);
                System.out.println("--> RequestMapping: " + url + " --> " + method);
            }
        }
    }

    /**
     * 首字母小写
     * 利用了ASCII码，大写字母和小写相差32这个规律转换首字母
     *
     * @param simpleName
     * @return
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}

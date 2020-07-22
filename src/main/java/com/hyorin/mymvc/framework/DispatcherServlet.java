package com.hyorin.mymvc.framework;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.hyorin.mymvc.controller.IndexController;
import com.hyorin.mymvc.controller.UserController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.ws.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


/**
 * 接收所有请求的servlet 总是映射到/
 * 根据controller方法定义的@get或@post的path来决定调用哪个方法
 * 获得方法modelAndView对象，渲染模板，写入httpServletResponse
 * 完成整个MVC的处理
 */
@WebServlet(urlPatterns = "/")
public class DispatcherServlet extends HttpServlet {

    //logger记录器 记录MVC框架消息
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /*请求路径到具体方法的映射*/
    private Map<String, GetDispatcher> getMappings = new HashMap<>();
    private Map<String, PostDispatcher> postMappings = new HashMap<>();
    //支持的Get请求参数类型
    private static final Set<Class<?>> supportedGetParameterType;
    //支持的Post请求参数类型
    private static final Set<Class<?>> supportedPostParameterType;
    // TODO: 可指定package自动扫描
    private static List<Class<?>> controllers = new ArrayList<>();


    //初始化参数集合
    static {
        controllers.add(UserController.class);
        controllers.add(IndexController.class);
        supportedGetParameterType = new HashSet<>();
        Collections.addAll(supportedGetParameterType, int.class, long.class, boolean.class, String.class,
                HttpSession.class, HttpServletResponse.class, HttpServletRequest.class);
        supportedPostParameterType = new HashSet<>();
        Collections.addAll(supportedPostParameterType, HttpSession.class, HttpServletResponse.class, HttpServletRequest.class);
    }

    //渲染模板引擎
    private ViewEngine viewEngine;

    /**
     * Servlet容器创建servlet对象之后 会自动调用init(ServletConfig)方法
     */
    @Override
    public void init() throws ServletException {
        logger.info("init{}...", getClass().getSimpleName());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //依次处理每个controller
        for (Class<?> controllerClass : controllers) {
            try {
                //反射获取controller类的实例
                Object controllerInstance = controllerClass.getConstructor().newInstance();
                //依次处理每个method
                for (Method method : controllerClass.getMethods()) {
                    if (method.getAnnotation(GetMapping.class) != null) {
                        //处理@get
                        if (method.getReturnType() != ModelAndView.class && method.getReturnType() != void.class) {
                            throw new UnsupportedOperationException(
                                    "unsupported return type" + method.getReturnType() + "for method " + method
                            );
                        }
                        for (Class<?> parameterClass : method.getParameterTypes()) {
                            if (!supportedGetParameterType.contains(parameterClass)) {
                                throw new UnsupportedOperationException(
                                        "unsupported parameter  type" + parameterClass + "for method " + method
                                );
                            }
                        }
                        String[] parameterNames = Arrays.stream(method.getParameters()).map(p -> p.getName()).toArray(String[]::new);
                        String path = method.getAnnotation(GetMapping.class).value();
                        logger.info("found GET {} => {}", path, method);
                        this.getMappings.put(path, new GetDispatcher(controllerInstance, method, parameterNames, method.getParameterTypes()));
                    } else if (method.getAnnotation(GetMapping.class) != null) {
                        //处理@Post
                        if (method.getReturnType() != ModelAndView.class && method.getReturnType() != void.class) {
                            throw new UnsupportedOperationException(
                                    "unsupported return type" + method.getReturnType() + "for method " + method
                            );
                        }
                        Class<?> requestBodyClass = null;
                        for (Class<?> parameterClass : method.getParameterTypes()) {
                            if (!supportedPostParameterType.contains(parameterClass)) {
                                if (requestBodyClass == null) {
                                    requestBodyClass = parameterClass;
                                } else {
                                    throw new UnsupportedOperationException(
                                            "unsupported duplicate request body type " + parameterClass + "for method " + method
                                    );
                                }
                            }
                        }
                        String path = method.getAnnotation(GetMapping.class).value();
                        logger.info("found Post: {} => {} ", path, method);
                        this.postMappings.put(path, new PostDispatcher(controllerInstance, method, method.getParameterTypes(), objectMapper));
                    }
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        //初始化模板渲染引擎
        this.viewEngine = new ViewEngine(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp, this.getMappings);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp, this.postMappings);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp,
                         Map<String, ? extends AbstractDispatcher> dispatcherMap) throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("utf-8");
        String path = req.getRequestURI().substring(req.getContextPath().length());
        AbstractDispatcher dispatcher = dispatcherMap.get(path);
        if (dispatcher == null) {
            resp.sendError(404);
            return;
        }
        ModelAndView mv = null;
        try {
            mv = dispatcher.invoke(req, resp);
        } catch (ReflectiveOperationException e) {
            throw new ServletException();
        }
        if (mv == null) {
            return;
        }
        if (mv.view.startsWith("redirect:")) {
            resp.sendRedirect(mv.view.substring(9));
            return;
        }
        PrintWriter pw = resp.getWriter();
        this.viewEngine.render(mv, pw);
        pw.flush();
    }
}

abstract class AbstractDispatcher {
    public abstract ModelAndView invoke(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ReflectiveOperationException;
}

/**
 * GetDispatcher对象处理真正的get请求
 */
class GetDispatcher extends AbstractDispatcher {

    final Object instance;    //Controller实例
    final Method method;      //Controller方法
    final String[] parameterNames;     //方法参数名称
    final Class<?>[] parameterClasses;     //方法参数类型

    public GetDispatcher(Object instance, Method method, String[] parameterNames, Class<?>[] parameterClasses) {
        super();
        this.instance = instance;
        this.method = method;
        this.parameterNames = parameterNames;
        this.parameterClasses = parameterClasses;
    }

    /**
     * 构造invoke方法需要的所有参数列表
     * 使用反射调用该方法后返回结果
     */
    @Override
    public ModelAndView invoke(HttpServletRequest req, HttpServletResponse resp) throws IOException, ReflectiveOperationException {
        Object[] arguments = new Object[parameterClasses.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            String parameterName = parameterNames[i];
            Class<?> parameterClass = parameterClasses[i];
            if (parameterClass == req.getClass()) {
                arguments[i] = req;
            } else if (parameterClass == resp.getClass()) {
                arguments[i] = resp;
            } else if (parameterClass == HttpSession.class) {
                arguments[i] = req.getSession();
            } else if (parameterClass == int.class) {
                arguments[i] = Integer.valueOf(getOrDefault(req, parameterName, "0"));
            } else if (parameterClass == long.class) {
                arguments[i] = Long.valueOf(getOrDefault(req, parameterName, "0"));
            } else if (parameterClass == boolean.class) {
                arguments[i] = Boolean.valueOf(getOrDefault(req, parameterName, "false"));
            } else if (parameterClass == String.class) {
                arguments[i] = getOrDefault(req, parameterName, "");
            } else {
                throw new RuntimeException("missing Handler for type:" + parameterClass);
            }

        }
        return (ModelAndView) this.method.invoke(this.instance, arguments);
    }

    private String getOrDefault(HttpServletRequest req, String name, String defaultValue) {
        String s = req.getParameter(name);
        return s == null ? defaultValue : s;
    }
}


/**
 * PostDispatcher对象处理真正的post请求
 * Post请求的所有数据都从post body读取
 * 这里简化处理 只支持json格式的post请求
 * json格式的post请求更容易转换成javabean
 */
class PostDispatcher extends AbstractDispatcher {
    final Object instance;    //Controller实例
    final Method method;      //Controller方法
    final Class<?>[] parameterClasses;     //方法参数类型
    ObjectMapper objectMapper;     //JSON映射

    public PostDispatcher(Object instance, Method method, Class<?>[] parameterClasses, ObjectMapper objectMapper) {
        this.instance = instance;
        this.method = method;
        this.parameterClasses = parameterClasses;
        this.objectMapper = objectMapper;
    }

    @Override
    public ModelAndView invoke(HttpServletRequest req, HttpServletResponse resp) throws IOException, ReflectiveOperationException {
        Object[] arguments = new Object[parameterClasses.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            Class<?> parameterClass = parameterClasses[i];
            if (parameterClass == HttpServletRequest.class) {
                arguments[i] = req;
            } else if (parameterClass == HttpServletResponse.class) {
                arguments[i] = resp;
            } else if (parameterClass == HttpSession.class) {
                arguments[i] = req.getSession();
            } else {
                //读取JSON并解析为javabean
                BufferedReader br = req.getReader();
                arguments[i] = this.objectMapper.readValue(br, parameterClass);
            }
        }
        return (ModelAndView) this.method.invoke(instance, arguments);
    }
}

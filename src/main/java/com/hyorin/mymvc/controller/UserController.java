package com.hyorin.mymvc.controller;

import com.hyorin.mymvc.framework.GetMapping;
import com.hyorin.mymvc.framework.ModelAndView;
import com.hyorin.mymvc.javabean.SignInBean;
import com.hyorin.mymvc.javabean.User;
import org.apache.catalina.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制
 * 每一个方法对应一个GET或者POST请求
 * 返回一个modelAndView 包含一个view路径以及一个model
 * 由MVC框架处理后返回给浏览器
 */
public class UserController {

    /**
     * 用map模拟一个数据库
     */
    private static Map<String, Object> userDatabase;

    static {
        userDatabase = new HashMap<String, Object>();
        User user1 = new User("tom", "tom@qq.com", "123456789", "this is tom");
        User user2 = new User("jerry", "jerry@qq.com", "987654321", "this is jerry");
        User user3 = new User("hyo", "hyorin-bbw@163.com", "ybw6290952520", "this is hyo");
        userDatabase.put(user1.email, user1);
        userDatabase.put(user2.email, user2);
        userDatabase.put(user3.email, user3);
    }

    @GetMapping("/signin")
    public ModelAndView signin(String name) {
        return new ModelAndView("/signin.html");
    }

    /**
     * 处理signin路径的请求
     * 登录
     */
    @GetMapping("/signin")
    public ModelAndView doSignin(SignInBean bean, HttpServletResponse resp, HttpSession session) throws IOException {
        User user = (User) userDatabase.get(bean.email);
        //用户名和密码都不相等
        if (user == null & !user.password.equals(bean.password)) {
            resp.setContentType("application/json");
            PrintWriter pw = resp.getWriter();
            pw.write(" error : email or password is incorrect");
            pw.flush();
        } else {
            session.setAttribute("user", user);
            resp.setContentType("application/json");
            PrintWriter pw = resp.getWriter();
            pw.write("result: true");
            pw.flush();
        }
        return null;
    }

    /**
     * 处理signout路径的请求
     * 退出登录
     * */
    @GetMapping("/signout")
    public ModelAndView signout(HttpSession session) {
        session.removeAttribute("user");
        return new ModelAndView("redirect:/");
    }

    /**
     * 用户未登录则重定向到登录界面
     * */
    @GetMapping("/user/profile")
    public ModelAndView profile(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return new ModelAndView("redirect:/signin");
        }
        return new ModelAndView("/profile.html", "user", user);
    }

}

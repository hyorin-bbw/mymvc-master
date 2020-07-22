package com.hyorin.mymvc.controller;

import com.hyorin.mymvc.framework.GetMapping;
import com.hyorin.mymvc.framework.ModelAndView;
import com.hyorin.mymvc.javabean.User;

import javax.servlet.http.HttpSession;

public class IndexController {

    @GetMapping("/")
    public ModelAndView index(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return new ModelAndView("/index.html", "user", user);
    }

    public ModelAndView hello(String name) {
        if (name == null) {
            name = "Guest";
        }
        return new ModelAndView("/hello.html", "name", name);
    }
}

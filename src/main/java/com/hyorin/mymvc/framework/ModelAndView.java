package com.hyorin.mymvc.framework;

import java.util.HashMap;
import java.util.Map;

/**
 * model and view
 * model为经servlet处理后的请求
 * view对应jsp渲染引擎
 */
public class ModelAndView {
    Map<String, Object> model;
    String view;

    public ModelAndView(String view) {
        this.view = view;
        this.model = new HashMap<String, Object>();
    }

    public ModelAndView(String view, String name, Object value) {
        this.view = view;
        model = new HashMap<String, Object>();
        model.put(name, value);
    }

    public ModelAndView(Map<String, Object> model, String view) {
        this.model = new HashMap<String, Object>(model);
        this.view = view;
    }
}

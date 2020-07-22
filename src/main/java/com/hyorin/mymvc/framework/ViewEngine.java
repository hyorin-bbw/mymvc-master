package com.hyorin.mymvc.framework;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ServletLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.Writer;

/**
 * 由pebbles实现的渲染引擎
 */
public class ViewEngine {

    private final PebbleEngine engine;

    public ViewEngine(ServletContext servletContext) {
        ServletLoader servletLoader = new ServletLoader(servletContext);
        servletLoader.setCharset("utf-8");
        servletLoader.setPrefix("/WEB-INF/templates");
        servletLoader.setSuffix("");
        this.engine = new PebbleEngine.Builder().autoEscaping(true).cacheActive(false).loader(servletLoader).build();
    }

    public void render(ModelAndView mv, Writer writer) throws IOException {
        PebbleTemplate template = this.engine.getTemplate(mv.view);
        template.evaluate(writer, mv.model);
    }
}

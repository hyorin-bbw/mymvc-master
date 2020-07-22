package com.hyorin.mymvc.framework;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 处理静态html文件
 * */
@WebServlet(urlPatterns = {"/favicon.icon", "/static/*"})
public class FileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletContext servletContext = req.getServletContext();
        //requestURL包含servletContext 需要去掉
        String urlPath = req.getRequestURI().substring(servletContext.getContextPath().length());
        //获取真实文件路径
        String filePath = servletContext.getRealPath(urlPath);
        if (filePath == null) {
            //获取路径错误404
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path path = Paths.get(filePath);
        if (path.toFile().isFile()) {
            //文件不存在
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        //根据文件名猜测目录
        String mime = Files.probeContentType(path);
        if (mime == null) {
            mime = "application/octet-stream";
        }
        resp.setContentType(mime);

        //读取文件写入响应
        OutputStream output = resp.getOutputStream();
        try (InputStream input = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] bytes = new byte[1024];
            int len;
            while ((len = input.read(bytes)) != -1) {
                output.write(bytes, 0, len);
            }
        }
        output.flush();
    }
}

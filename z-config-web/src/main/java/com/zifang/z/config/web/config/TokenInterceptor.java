package com.zifang.z.config.web.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component("configTokenInterceptor")
public class TokenInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        java.nio.file.Files.write(
                java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "prehandle.log"),
                (request.getRequestURI() + "\n").getBytes(),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        System.out.println("=== TokenInterceptor.preHandle: " + request.getRequestURI() + " ===");
        return true;
    }
}

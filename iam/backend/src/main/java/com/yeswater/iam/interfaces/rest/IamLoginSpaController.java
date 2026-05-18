package com.yeswater.iam.interfaces.rest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 返回 yeds-login-web 构建的 index.html（/iam/login 入口）。
 */
@RestController
public class IamLoginSpaController {

    /**
     * 统一登录 SPA 入口页。
     */
    @GetMapping(value = {"/iam/login", "/iam/login/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> loginSpaIndex() {
        Resource index = new ClassPathResource("static/yeds-login/index.html");
        if (!index.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(index);
    }
}

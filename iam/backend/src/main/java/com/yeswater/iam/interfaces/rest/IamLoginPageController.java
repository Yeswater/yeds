package com.yeswater.iam.interfaces.rest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IamLoginPageController {

    /**
     * IAM 统一登录页，登录成功后回跳业务系统。
     */
    @GetMapping(value = "/iam/login", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> loginPage(
            @RequestParam(value = "redirect_uri", required = false) String redirectUri
    ) {
        String targetRedirectUri = escapeHtml(redirectUri == null || redirectUri.isBlank()
                ? "http://127.0.0.1:5181/auth/sso-callback?redirect=/abac/policy"
                : redirectUri);
        String htmlTemplate = """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>IAM 统一登录</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 0; background: #f5f7fa; }
                    .wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; }
                    .card { width: 360px; background: #fff; border-radius: 10px; box-shadow: 0 8px 20px rgba(0,0,0,0.08); padding: 24px; }
                    h1 { margin: 0 0 20px; font-size: 20px; text-align: center; color: #303133; }
                    .item { margin-bottom: 12px; }
                    label { display: block; margin-bottom: 6px; color: #606266; font-size: 14px; }
                    input { width: 100%; box-sizing: border-box; border: 1px solid #dcdfe6; border-radius: 6px; padding: 10px; font-size: 14px; }
                    button { width: 100%; margin-top: 14px; border: none; border-radius: 6px; padding: 10px; background: #409eff; color: #fff; font-size: 14px; cursor: pointer; }
                    button:disabled { opacity: .7; cursor: not-allowed; }
                    .error { color: #f56c6c; min-height: 22px; margin-top: 10px; font-size: 13px; text-align: center; }
                  </style>
                </head>
                <body>
                <div class="wrap">
                  <div class="card">
                    <h1>IAM 统一登录</h1>
                    <div class="item">
                      <label for="username">账号</label>
                      <input id="username" name="username" autocomplete="username" value="admin" />
                    </div>
                    <div class="item">
                      <label for="password">密码</label>
                      <input id="password" name="password" type="password" autocomplete="current-password" value="admin123" />
                    </div>
                    <button id="submit-btn">登 录</button>
                    <div class="error" id="error-tip"></div>
                  </div>
                </div>
                <script>
                  const redirectUri = "__REDIRECT_URI__";
                  const submitBtn = document.getElementById('submit-btn');
                  const errorTip = document.getElementById('error-tip');
                  submitBtn.addEventListener('click', async () => {
                    const username = document.getElementById('username').value.trim();
                    const password = document.getElementById('password').value.trim();
                    if (!username || !password) {
                      errorTip.textContent = '请输入账号和密码';
                      return;
                    }
                    submitBtn.disabled = true;
                    errorTip.textContent = '';
                    try {
                      const response = await fetch('/api/iam/auth/login', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ username, password })
                      });
                      const payload = await response.json().catch(() => ({}));
                      if (!response.ok) {
                        throw new Error(payload.message || ('登录失败: ' + response.status));
                      }
                      const target = new URL(redirectUri);
                      target.searchParams.set('access_token', payload.accessToken || '');
                      target.searchParams.set('refresh_token', payload.refreshToken || '');
                      target.searchParams.set('expires_in', String(payload.expiresIn || 0));
                      target.searchParams.set('username', username);
                      window.location.replace(target.toString());
                    } catch (error) {
                      errorTip.textContent = error?.message || '登录失败';
                      submitBtn.disabled = false;
                    }
                  });
                </script>
                </body>
                </html>
                """;
        String html = htmlTemplate.replace("__REDIRECT_URI__", targetRedirectUri);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

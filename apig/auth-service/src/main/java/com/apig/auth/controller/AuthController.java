package com.apig.auth.controller;

import com.apig.auth.config.AuthProperties;
import com.apig.auth.model.ClientInfo;
import com.apig.auth.model.TokenRequest;
import com.apig.auth.model.TokenResponse;
import com.apig.auth.service.ClientCredentialService;
import com.apig.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ClientCredentialService clientCredentialService;
    private final JwtService jwtService;
    private final AuthProperties authProperties;

    public AuthController(ClientCredentialService clientCredentialService,
                          JwtService jwtService,
                          AuthProperties authProperties) {
        this.clientCredentialService = clientCredentialService;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
    }

    /**
     * 基于 clientId/clientSecret 签发访问令牌。
     */
    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        ClientInfo clientInfo = clientCredentialService.findByClientId(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_client"));
        if (!clientCredentialService.verifySecret(clientInfo, request.clientSecret())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_client_secret");
        }
        return new TokenResponse(
                jwtService.generateClientToken(clientInfo),
                "Bearer",
                authProperties.tokenExpireSeconds()
        );
    }

    /**
     * 健康检查接口。
     */
    @GetMapping("/health")
    @ResponseStatus(HttpStatus.OK)
    public String health() {
        return "ok";
    }
}

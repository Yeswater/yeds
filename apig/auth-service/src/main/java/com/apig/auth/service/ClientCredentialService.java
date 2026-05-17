package com.apig.auth.service;

import com.apig.auth.model.ClientInfo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ClientCredentialService {

    private final Map<String, ClientInfo> clients = Map.of(
            "portal-client", new ClientInfo("portal-client", "portal-secret", "tenant-a", Set.of("apig.read", "apig.write")),
            "readonly-client", new ClientInfo("readonly-client", "readonly-secret", "tenant-a", Set.of("apig.read"))
    );

    /**
     * 按 clientId 查询客户端信息。
     */
    public Optional<ClientInfo> findByClientId(String clientId) {
        return Optional.ofNullable(clients.get(clientId));
    }

    /**
     * 校验客户端密钥是否匹配。
     */
    public boolean verifySecret(ClientInfo info, String clientSecret) {
        return info.clientSecret().equals(clientSecret);
    }
}

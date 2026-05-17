package com.yeswater.bids.exec.infrastructure.dialect;

import com.yeswater.foundation.common.web.ApiException;
import com.yeswater.bids.sql.dialect.SqlDialectType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RelationalDatabaseDialectRegistry {
    private final Map<SqlDialectType, RelationalDatabaseDialectPlugin> plugins;

    public RelationalDatabaseDialectRegistry(List<RelationalDatabaseDialectPlugin> pluginList) {
        EnumMap<SqlDialectType, RelationalDatabaseDialectPlugin> map = new EnumMap<>(SqlDialectType.class);
        for (RelationalDatabaseDialectPlugin plugin : pluginList) {
            for (SqlDialectType dialectType : plugin.dialectTypes()) {
                if (map.put(dialectType, plugin) != null) {
                    throw new IllegalStateException("重复的方言插件注册：" + dialectType);
                }
            }
        }
        for (SqlDialectType dialectType : SqlDialectType.values()) {
            if (!map.containsKey(dialectType)) {
                throw new IllegalStateException("缺少方言插件：" + dialectType);
            }
        }
        this.plugins = Map.copyOf(map);
    }

    public RelationalDatabaseDialectPlugin require(SqlDialectType dialectType) {
        RelationalDatabaseDialectPlugin plugin = plugins.get(dialectType);
        if (plugin == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "未注册方言插件：" + dialectType);
        }
        return plugin;
    }
}

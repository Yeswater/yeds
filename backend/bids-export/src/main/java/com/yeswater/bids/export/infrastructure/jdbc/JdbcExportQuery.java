package com.yeswater.bids.export.infrastructure.jdbc;

import com.yeswater.bids.export.api.ExportColumnSpec;
import com.yeswater.bids.export.api.ExportDataSourceSpec;
import com.yeswater.bids.export.api.ExportJobRequest;
import com.yeswater.bids.export.infrastructure.config.ExportProperties;
import com.yeswater.bids.export.infrastructure.excel.RowMasker;
import com.yeswater.bids.export.infrastructure.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Component
public class JdbcExportQuery {
    private final ExportProperties properties;

    public JdbcExportQuery(ExportProperties properties) {
        this.properties = properties;
    }

    public Long countWithTimeout(ExportJobRequest request) {
        String countSql = "SELECT COUNT(*) FROM (" + request.finalSql() + ") bids_export_cnt";
        MapSqlParameterSource params = new MapSqlParameterSource(ExportJdbcParameters.normalize(request.parameters()));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Long> future = executor.submit((Callable<Long>) () -> {
                NamedParameterJdbcTemplate jdbc = createJdbc(request.dataSource());
                Long count = jdbc.queryForObject(countSql, params, Long.class);
                return count == null ? 0L : count;
            });
            return future.get(properties.getCountTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return null;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "统计行数失败：" + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    public void streamRows(ExportJobRequest request, int maxRows, Consumer<Map<String, Object>> consumer) {
        List<ExportColumnSpec> visible = request.columns().stream()
                .filter(ExportColumnSpec::visible)
                .sorted(Comparator.comparingInt(ExportColumnSpec::sortOrder))
                .toList();
        MapSqlParameterSource params = new MapSqlParameterSource(ExportJdbcParameters.normalize(request.parameters()));
        NamedParameterJdbcTemplate jdbc = createJdbc(request.dataSource());
        jdbc.getJdbcTemplate().setFetchSize(properties.getFetchSize());
        final int[] written = {0};
        jdbc.query(request.finalSql(), params, rs -> {
            if (written[0] >= maxRows) {
                return;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            if (visible.isEmpty()) {
                int count = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= count; i++) {
                    row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
            } else {
                for (ExportColumnSpec column : visible) {
                    Object value = rs.getObject(column.columnName());
                    row.put(column.columnName(), RowMasker.mask(value, column.maskType()));
                }
            }
            consumer.accept(row);
            written[0]++;
        });
    }

    private NamedParameterJdbcTemplate createJdbc(ExportDataSourceSpec spec) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(spec.driverClassName());
        dataSource.setUrl(spec.jdbcUrl());
        dataSource.setUsername(spec.username());
        dataSource.setPassword(spec.password());
        return new NamedParameterJdbcTemplate(dataSource);
    }
}

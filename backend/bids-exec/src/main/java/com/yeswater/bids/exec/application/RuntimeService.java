package com.yeswater.bids.exec.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeswater.bids.exec.infrastructure.audit.AuditSearchPublisher;
import com.yeswater.bids.exec.infrastructure.web.ApiException;
import com.yeswater.bids.exec.interfaces.dto.ExecuteRequest;
import com.yeswater.bids.exec.interfaces.dto.ExecuteResponse;
import com.yeswater.bids.exec.interfaces.dto.FormOptionItem;
import com.yeswater.bids.exec.interfaces.dto.FormFieldResponse;
import com.yeswater.bids.exec.interfaces.dto.FormResponse;
import com.yeswater.bids.exec.interfaces.dto.LogResponse;
import com.yeswater.bids.exec.interfaces.dto.ResultColumnResponse;
import com.yeswater.bids.exec.domain.model.DataSourceConfig;
import com.yeswater.bids.exec.domain.model.ExecuteLog;
import com.yeswater.bids.exec.domain.model.FieldType;
import com.yeswater.bids.exec.domain.model.FormField;
import com.yeswater.bids.exec.domain.model.ModelPermission;
import com.yeswater.bids.exec.domain.model.ResultColumn;
import com.yeswater.bids.exec.domain.model.SqlModelConfig;
import com.yeswater.bids.exec.domain.model.SqlModelStatus;
import com.yeswater.bids.exec.infrastructure.datasource.BusinessDataSourceManager;
import com.yeswater.bids.exec.infrastructure.dialect.RelationalDatabaseDialectPlugin;
import com.yeswater.bids.exec.infrastructure.dialect.RelationalDatabaseDialectRegistry;
import com.yeswater.bids.exec.infrastructure.persistence.ConfigQueryRepository;
import com.yeswater.bids.sql.dialect.SqlDialectType;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RuntimeService {
    private static final Pattern SAFE_SQL_IDENT = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final ConfigQueryRepository configRepository;
    private final SqlTemplateService sqlTemplateService;
    private final SqlSafetyValidator sqlSafetyValidator;
    private final SqlDisplayFormatter sqlDisplayFormatter;
    private final BusinessDataSourceManager dataSourceManager;
    private final RelationalDatabaseDialectRegistry dialectRegistry;
    private final AuditSearchPublisher auditSearchPublisher;
    private final ObjectMapper objectMapper;

    public RuntimeService(
            ConfigQueryRepository configRepository,
            SqlTemplateService sqlTemplateService,
            SqlSafetyValidator sqlSafetyValidator,
            SqlDisplayFormatter sqlDisplayFormatter,
            BusinessDataSourceManager dataSourceManager,
            RelationalDatabaseDialectRegistry dialectRegistry,
            AuditSearchPublisher auditSearchPublisher,
            ObjectMapper objectMapper
    ) {
        this.configRepository = configRepository;
        this.sqlTemplateService = sqlTemplateService;
        this.sqlSafetyValidator = sqlSafetyValidator;
        this.sqlDisplayFormatter = sqlDisplayFormatter;
        this.dataSourceManager = dataSourceManager;
        this.dialectRegistry = dialectRegistry;
        this.auditSearchPublisher = auditSearchPublisher;
        this.objectMapper = objectMapper;
    }

    public FormResponse getForm(String modelCode) {
        SqlModelConfig config = requirePublishedModel(modelCode);
        ensurePermission(config);
        NamedParameterJdbcTemplate jdbc = dataSourceManager.jdbcTemplate(config.model().datasourceCode());
        List<ModelPermission> perms = config.permissions();
        return new FormResponse(
                config.model().code(),
                config.model().name(),
                config.fields().stream().map(f -> toFieldResponse(f, buildFieldOptions(jdbc, f, perms))).toList(),
                config.columns().stream().map(this::toColumnResponse).toList()
        );
    }

    public ExecuteResponse execute(String modelCode, ExecuteRequest request) {
        String executeId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        String finalSql = "";
        Map<String, Object> parameters = request != null && request.parameters() != null ? request.parameters() : Map.of();
        Integer reqPage = request != null ? request.currentPage() : null;
        Integer reqSize = request != null ? request.pageSize() : null;
        int currentPage = reqPage != null && reqPage > 0 ? reqPage : 1;
        int pageSize = reqSize != null && reqSize > 0 ? reqSize : 200;
        try {
            SqlModelConfig config = requirePublishedModel(modelCode);
            ensurePermission(config);
            Map<String, Object> bindParameters = validateAndConvertParameters(config.fields(), parameters);
            String renderedSql = sqlTemplateService.render(config.model().sqlTemplate(), bindParameters);
            DataSourceConfig dataSource = configRepository.findDataSource(config.model().datasourceCode())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "数据源不存在：" + config.model().datasourceCode()));
            SqlDialectType dialect = dataSource.sqlDialect();
            RelationalDatabaseDialectPlugin dialectPlugin = dialectRegistry.require(dialect);
            sqlSafetyValidator.validateReadonlySelect(renderedSql, dialect);

            NamedParameterJdbcTemplate jdbcTemplate = dataSourceManager.jdbcTemplate(config.model().datasourceCode());

            MapSqlParameterSource countSource = new MapSqlParameterSource(bindParameters);
            String countSql = "SELECT COUNT(*) FROM (" + renderedSql + ") bids_cnt";
            Long total = jdbcTemplate.queryForObject(countSql, countSource, Long.class);
            if (total == null) {
                total = 0L;
            }

            long offsetLong = (long) (currentPage - 1) * pageSize;
            if (offsetLong < 0 || offsetLong > Integer.MAX_VALUE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "分页偏移越界");
            }
            int offset = (int) offsetLong;

            String pagedSql =
                    dialectPlugin.wrapSelectWithPaging(renderedSql, "__bids_page_size", "__bids_offset");
            Map<String, Object> queryParameters = new HashMap<>(bindParameters);
            queryParameters.put("__bids_page_size", pageSize);
            queryParameters.put("__bids_offset", offset);

            finalSql = sqlDisplayFormatter.toDisplaySql(pagedSql, queryParameters);

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(pagedSql, queryParameters);
            List<Map<String, Object>> visibleRows = rows.stream()
                    .map(row -> filterAndMaskRow(row, config.columns()))
                    .toList();
            long durationMs = System.currentTimeMillis() - start;
            saveAudit(executeId, modelCode, currentUsername(), finalSql, bindParameters, true, null, durationMs, visibleRows.size());
            return new ExecuteResponse(
                    executeId,
                    finalSql,
                    config.columns().stream().filter(ResultColumn::visible).map(this::toColumnResponse).toList(),
                    visibleRows,
                    visibleRows.size(),
                    durationMs,
                    currentPage,
                    pageSize,
                    total
            );
        } catch (Exception exception) {
            long durationMs = System.currentTimeMillis() - start;
            saveAudit(executeId, modelCode, currentUsername(), finalSql, parameters, false, exception.getMessage(), durationMs, 0);
            throw exception;
        }
    }

    /**
     * 校验并渲染导出用 SQL（全量，不分页）。
     */
    public PreparedExport prepareExport(String modelCode, Map<String, Object> parameters) {
        SqlModelConfig config = requirePublishedModel(modelCode);
        ensurePermission(config);
        Map<String, Object> bindParameters = validateAndConvertParameters(config.fields(), parameters != null ? parameters : Map.of());
        String renderedSql = sqlTemplateService.render(config.model().sqlTemplate(), bindParameters);
        DataSourceConfig dataSource = configRepository.findDataSource(config.model().datasourceCode())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "数据源不存在：" + config.model().datasourceCode()));
        sqlSafetyValidator.validateReadonlySelect(renderedSql, dataSource.sqlDialect());
        return new PreparedExport(config.model(), dataSource, renderedSql, bindParameters, config.columns());
    }

    public LogResponse getLog(String executeId) {
        ExecuteLog log = configRepository.findLog(executeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "执行日志不存在：" + executeId));
        return new LogResponse(
                log.executeId(),
                log.modelCode(),
                log.username(),
                log.finalSql(),
                log.parametersJson(),
                log.success(),
                log.errorMessage(),
                log.durationMs(),
                log.rowCount(),
                log.createdAt()
        );
    }

    private SqlModelConfig requirePublishedModel(String modelCode) {
        SqlModelConfig config = configRepository.findModelByCode(modelCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SQL 模型不存在：" + modelCode));
        if (config.model().status() != SqlModelStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SQL 模型未发布：" + modelCode);
        }
        return config;
    }

    private void ensurePermission(SqlModelConfig config) {
        List<ModelPermission> permissions = config.permissions();
        if (permissions.isEmpty()) {
            return;
        }
        String username = currentUsername();
        Set<String> roles = currentRoles();
        boolean allowed = permissions.stream().anyMatch(permission ->
                username.equals(permission.username())
                        || roles.contains(permission.roleCode())
                        || roles.contains("ROLE_" + permission.roleCode())
                        || "ALL".equalsIgnoreCase(permission.roleCode()));
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "没有模型执行权限：" + config.model().code());
        }
    }

    private Map<String, Object> validateAndConvertParameters(List<FormField> fields, Map<String, Object> requestParameters) {
        Map<String, FormField> fieldMap = fields.stream()
                .collect(Collectors.toMap(FormField::fieldName, Function.identity()));
        for (String name : requestParameters.keySet()) {
            if (!fieldMap.containsKey(name)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "参数不在白名单内：" + name);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (FormField field : fields) {
            Object value = requestParameters.get(field.fieldName());
            if (isBlank(value) && field.defaultValue() != null && !field.defaultValue().isBlank()) {
                value = field.defaultValue();
            }
            if (field.required() && isBlank(value)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "必填参数不能为空：" + field.fieldName());
            }
            if (!isBlank(value)) {
                result.put(field.fieldName(), convertValue(field.fieldType(), value, field.fieldName()));
            }
        }
        return result;
    }

    private Object convertValue(FieldType fieldType, Object value, String fieldName) {
        try {
            return switch (fieldType) {
                case TEXT, SELECT -> String.valueOf(value);
                case NUMBER -> new BigDecimal(String.valueOf(value));
                case BOOLEAN -> value instanceof Boolean booleanValue ? booleanValue : Boolean.parseBoolean(String.valueOf(value));
                case DATE -> LocalDate.parse(String.valueOf(value));
                case DATETIME -> LocalDateTime.parse(String.valueOf(value));
            };
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "参数类型不合法：" + fieldName);
        }
    }

    private Map<String, Object> filterAndMaskRow(Map<String, Object> row, List<ResultColumn> columns) {
        if (columns.isEmpty()) {
            return row;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (ResultColumn column : columns) {
            if (!column.visible()) {
                continue;
            }
            Object value = row.get(column.columnName());
            result.put(column.columnName(), mask(value, column.maskType()));
        }
        return result;
    }

    private Object mask(Object value, String maskType) {
        if (value == null || maskType == null || maskType.isBlank()) {
            return value;
        }
        String text = String.valueOf(value);
        return switch (maskType.toUpperCase()) {
            case "FULL" -> "******";
            case "PHONE" -> text.length() <= 7 ? "******" : text.substring(0, 3) + "****" + text.substring(text.length() - 4);
            case "EMAIL" -> maskEmail(text);
            default -> value;
        };
    }

    private String maskEmail(String text) {
        int at = text.indexOf('@');
        if (at <= 1) {
            return "******";
        }
        return text.charAt(0) + "****" + text.substring(at);
    }

    private void saveAudit(
            String executeId,
            String modelCode,
            String username,
            String finalSql,
            Map<String, Object> parameters,
            boolean success,
            String errorMessage,
            long durationMs,
            int rowCount
    ) {
        ExecuteLog log = new ExecuteLog(
                UUID.randomUUID().toString(),
                executeId,
                modelCode,
                username,
                finalSql == null ? "" : finalSql,
                toJson(parameters),
                success,
                errorMessage,
                durationMs,
                rowCount,
                null
        );
        configRepository.saveExecuteLog(log);
        try {
            auditSearchPublisher.publish(log);
        } catch (Exception ignored) {
            // Elasticsearch 审计是旁路能力，不能影响主查询链路。
        }
    }

    private String toJson(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    /**
     * 组装字段下拉项：distinctFrom 动态值 + 可选的模型权限项（optionsJson.permissionOptions 为 true 时）。
     */
    private List<FormOptionItem> buildFieldOptions(
            NamedParameterJdbcTemplate jdbc, FormField field, List<ModelPermission> permissions) {
        List<FormOptionItem> distinct = resolveDistinctOptions(jdbc, field);
        if (!permissionOptionsRequested(field)) {
            return distinct;
        }
        List<FormOptionItem> perm = toPermissionOptionItems(permissions);
        if (perm.isEmpty()) {
            return distinct;
        }
        LinkedHashMap<String, FormOptionItem> merged = new LinkedHashMap<>();
        for (FormOptionItem p : perm) {
            merged.put(p.value(), p);
        }
        for (FormOptionItem d : distinct) {
            merged.putIfAbsent(d.value(), d);
        }
        return new ArrayList<>(merged.values());
    }

    /** optionsJson 根节点 permissionOptions 为 true 时，将 bids_model_permission 中的用户/角色并入下拉。 */
    private boolean permissionOptionsRequested(FormField field) {
        String raw = field.optionsJson();
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            return root.path("permissionOptions").asBoolean(false);
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<FormOptionItem> toPermissionOptionItems(List<ModelPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, FormOptionItem> map = new LinkedHashMap<>();
        for (ModelPermission p : permissions) {
            if (p.username() != null && !p.username().isBlank()) {
                String u = p.username();
                map.putIfAbsent("perm:user:" + u, new FormOptionItem("用户 · " + u, u));
            }
            if (p.roleCode() != null && !p.roleCode().isBlank()) {
                String r = p.roleCode();
                map.putIfAbsent("perm:role:" + r, new FormOptionItem("角色 · " + r, r));
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 根据字段 optionsJson 中的 distinctFrom 配置，从业务数据源查询去重值作为下拉选项。
     */
    private List<FormOptionItem> resolveDistinctOptions(NamedParameterJdbcTemplate jdbc, FormField field) {
        String raw = field.optionsJson();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode df = root.get("distinctFrom");
            if (df == null || !df.isObject()) {
                return List.of();
            }
            String table = textOrNull(df, "table");
            String column = textOrNull(df, "column");
            if (table == null || column == null
                    || !SAFE_SQL_IDENT.matcher(table).matches()
                    || !SAFE_SQL_IDENT.matcher(column).matches()) {
                return List.of();
            }
            String sql = "SELECT DISTINCT " + column + " AS v FROM " + table
                    + " WHERE " + column + " IS NOT NULL ORDER BY 1";
            return jdbc.query(sql, (rs, rowNum) -> {
                String v = rs.getString("v");
                String label = v == null ? "" : v;
                return new FormOptionItem(label, label);
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String textOrNull(JsonNode node, String key) {
        if (!node.has(key) || node.get(key).isNull()) {
            return null;
        }
        String t = node.get(key).asText();
        return t.isBlank() ? null : t;
    }

    private FormFieldResponse toFieldResponse(FormField field, List<FormOptionItem> optionItems) {
        return new FormFieldResponse(
                field.fieldName(),
                field.label(),
                field.fieldType(),
                field.required(),
                field.defaultValue(),
                field.optionsJson(),
                field.sortOrder(),
                optionItems
        );
    }

    private ResultColumnResponse toColumnResponse(ResultColumn column) {
        return new ResultColumnResponse(
                column.columnName(),
                column.label(),
                column.valueType(),
                column.visible(),
                column.maskType(),
                column.sortOrder()
        );
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }

    private Set<String> currentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}

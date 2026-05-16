package com.yeswater.bids.export.infrastructure.persistence;

import com.yeswater.bids.export.domain.model.ExportTask;
import com.yeswater.bids.export.domain.model.ExportTaskStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ExportTaskRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public ExportTaskRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(ExportTask task) {
        jdbc.update("""
                insert into bids_export_task
                (id, model_code, username, parameters_json, final_sql, status, mode, file_format,
                 estimated_rows, actual_rows, truncated, progress_pct, error_message,
                 rustfs_bucket, rustfs_object_key, file_size_bytes, download_expires_at,
                 created_at, updated_at, finished_at)
                values
                (:id, :modelCode, :username, :parametersJson, :finalSql, :status, :mode, :fileFormat,
                 :estimatedRows, :actualRows, :truncated, :progressPct, :errorMessage,
                 :rustfsBucket, :rustfsObjectKey, :fileSizeBytes, :downloadExpiresAt,
                 :createdAt, :updatedAt, :finishedAt)
                """, toParams(task));
    }

    public void update(ExportTask task) {
        jdbc.update("""
                update bids_export_task set
                  status = :status,
                  actual_rows = :actualRows,
                  truncated = :truncated,
                  progress_pct = :progressPct,
                  error_message = :errorMessage,
                  rustfs_bucket = :rustfsBucket,
                  rustfs_object_key = :rustfsObjectKey,
                  file_size_bytes = :fileSizeBytes,
                  download_expires_at = :downloadExpiresAt,
                  updated_at = :updatedAt,
                  finished_at = :finishedAt
                where id = :id
                """, toParams(task));
    }

    public Optional<ExportTask> findById(String id) {
        return jdbc.query("""
                select id, model_code, username, parameters_json, final_sql, status, mode, file_format,
                       estimated_rows, actual_rows, truncated, progress_pct, error_message,
                       rustfs_bucket, rustfs_object_key, file_size_bytes, download_expires_at,
                       created_at, updated_at, finished_at
                from bids_export_task where id = :id
                """, new MapSqlParameterSource("id", id), mapper()).stream().findFirst();
    }

    public long countRunningByUser(String username) {
        Long count = jdbc.queryForObject("""
                select count(*) from bids_export_task
                where username = :username and status = 'RUNNING'
                """, new MapSqlParameterSource("username", username), Long.class);
        return count == null ? 0 : count;
    }

    public List<ExportTask> listByUser(String username, int limit) {
        return jdbc.query("""
                select id, model_code, username, parameters_json, final_sql, status, mode, file_format,
                       estimated_rows, actual_rows, truncated, progress_pct, error_message,
                       rustfs_bucket, rustfs_object_key, file_size_bytes, download_expires_at,
                       created_at, updated_at, finished_at
                from bids_export_task
                where username = :username
                order by created_at desc
                limit :limit
                """, new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("limit", limit), mapper());
    }

    private MapSqlParameterSource toParams(ExportTask task) {
        return new MapSqlParameterSource()
                .addValue("id", task.id())
                .addValue("modelCode", task.modelCode())
                .addValue("username", task.username())
                .addValue("parametersJson", task.parametersJson())
                .addValue("finalSql", task.finalSql())
                .addValue("status", task.status().name())
                .addValue("mode", task.mode())
                .addValue("fileFormat", task.fileFormat())
                .addValue("estimatedRows", task.estimatedRows())
                .addValue("actualRows", task.actualRows())
                .addValue("truncated", task.truncated())
                .addValue("progressPct", task.progressPct())
                .addValue("errorMessage", task.errorMessage())
                .addValue("rustfsBucket", task.rustfsBucket())
                .addValue("rustfsObjectKey", task.rustfsObjectKey())
                .addValue("fileSizeBytes", task.fileSizeBytes())
                .addValue("downloadExpiresAt", task.downloadExpiresAt() == null ? null : Timestamp.from(task.downloadExpiresAt()))
                .addValue("createdAt", Timestamp.from(task.createdAt()))
                .addValue("updatedAt", Timestamp.from(task.updatedAt()))
                .addValue("finishedAt", task.finishedAt() == null ? null : Timestamp.from(task.finishedAt()));
    }

    private RowMapper<ExportTask> mapper() {
        return (rs, rowNum) -> new ExportTask(
                rs.getString("id"),
                rs.getString("model_code"),
                rs.getString("username"),
                rs.getString("parameters_json"),
                rs.getString("final_sql"),
                ExportTaskStatus.valueOf(rs.getString("status")),
                rs.getString("mode"),
                rs.getString("file_format"),
                (Long) rs.getObject("estimated_rows"),
                (Long) rs.getObject("actual_rows"),
                rs.getBoolean("truncated"),
                rs.getInt("progress_pct"),
                rs.getString("error_message"),
                rs.getString("rustfs_bucket"),
                rs.getString("rustfs_object_key"),
                (Long) rs.getObject("file_size_bytes"),
                toInstant(rs.getTimestamp("download_expires_at")),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                toInstant(rs.getTimestamp("finished_at"))
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}

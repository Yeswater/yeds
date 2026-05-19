package com.yeswater.alb.controlplane.repository;

import com.yeswater.alb.controlplane.domain.HeaderPolicy;
import com.yeswater.alb.controlplane.domain.Route;
import com.yeswater.alb.controlplane.domain.Upstream;
import com.yeswater.alb.controlplane.snapshot.RoutingSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class AlbJdbcRepository {

    private static final RowMapper<Upstream> UPSTREAM_MAPPER = (rs, rowNum) -> new Upstream(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("target_url"),
            rs.getInt("websocket_enabled") == 1,
            rs.getString("remark"),
            toInstant(rs.getTimestamp("gmt_create")),
            toInstant(rs.getTimestamp("gmt_modified"))
    );

    private static final RowMapper<Route> ROUTE_MAPPER = (rs, rowNum) -> new Route(
            rs.getLong("id"),
            rs.getString("env"),
            rs.getString("host"),
            rs.getString("path_pattern"),
            rs.getInt("priority"),
            rs.getLong("upstream_id"),
            rs.getString("upstream_name"),
            rs.getString("target_url"),
            rs.getInt("enabled") == 1,
            rs.getInt("strip_prefix") == 1,
            rs.getString("redirect_url"),
            rs.getInt("system_locked") == 1,
            rs.getString("remark"),
            toInstant(rs.getTimestamp("gmt_create")),
            toInstant(rs.getTimestamp("gmt_modified"))
    );

    private static final RowMapper<HeaderPolicy> HEADER_MAPPER = (rs, rowNum) -> new HeaderPolicy(
            rs.getLong("id"),
            rs.getLong("route_id"),
            rs.getString("direction"),
            rs.getString("op"),
            rs.getString("header_key"),
            rs.getString("header_value"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1,
            toInstant(rs.getTimestamp("gmt_create")),
            toInstant(rs.getTimestamp("gmt_modified"))
    );

    private final JdbcTemplate jdbcTemplate;

    public AlbJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Upstream> listUpstreams() {
        return jdbcTemplate.query(
                "SELECT * FROM alb_upstream ORDER BY id",
                UPSTREAM_MAPPER);
    }

    public Optional<Upstream> findUpstream(Long id) {
        List<Upstream> rows = jdbcTemplate.query(
                "SELECT * FROM alb_upstream WHERE id = ?",
                UPSTREAM_MAPPER,
                id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long insertUpstream(String name, String targetUrl, boolean websocketEnabled, String remark) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO alb_upstream(name, target_url, websocket_enabled, remark) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, targetUrl);
            ps.setInt(3, websocketEnabled ? 1 : 0);
            ps.setString(4, remark);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public int updateUpstream(Long id, String name, String targetUrl, boolean websocketEnabled, String remark) {
        return jdbcTemplate.update(
                "UPDATE alb_upstream SET name=?, target_url=?, websocket_enabled=?, remark=?, gmt_modified=CURRENT_TIMESTAMP WHERE id=?",
                name, targetUrl, websocketEnabled ? 1 : 0, remark, id);
    }

    public int deleteUpstream(Long id) {
        return jdbcTemplate.update("DELETE FROM alb_upstream WHERE id = ?", id);
    }

    public boolean upstreamReferenced(Long upstreamId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alb_route WHERE upstream_id = ?",
                Integer.class,
                upstreamId);
        return count != null && count > 0;
    }

    public List<Route> listRoutes() {
        return jdbcTemplate.query(
                """
                SELECT r.*, u.name AS upstream_name, u.target_url
                FROM alb_route r
                JOIN alb_upstream u ON u.id = r.upstream_id
                ORDER BY r.host, r.priority DESC, r.id
                """,
                ROUTE_MAPPER);
    }

    public Optional<Route> findRoute(Long id) {
        List<Route> rows = jdbcTemplate.query(
                """
                SELECT r.*, u.name AS upstream_name, u.target_url
                FROM alb_route r
                JOIN alb_upstream u ON u.id = r.upstream_id
                WHERE r.id = ?
                """,
                ROUTE_MAPPER,
                id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public boolean isRouteSystemLocked(Long id) {
        Integer locked = jdbcTemplate.queryForObject(
                "SELECT system_locked FROM alb_route WHERE id = ?",
                Integer.class,
                id);
        return locked != null && locked == 1;
    }

    public long insertRoute(String env, String host, String pathPattern, int priority, Long upstreamId,
                           boolean enabled, boolean stripPrefix, String redirectUrl, String remark) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO alb_route(env, host, path_pattern, priority, upstream_id, enabled, strip_prefix, redirect_url, remark)
                    VALUES (?,?,?,?,?,?,?,?,?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, env);
            ps.setString(2, host);
            ps.setString(3, pathPattern);
            ps.setInt(4, priority);
            ps.setLong(5, upstreamId);
            ps.setInt(6, enabled ? 1 : 0);
            ps.setInt(7, stripPrefix ? 1 : 0);
            ps.setString(8, redirectUrl);
            ps.setString(9, remark);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public int updateRoute(Long id, String env, String host, String pathPattern, int priority, Long upstreamId,
                           boolean enabled, boolean stripPrefix, String redirectUrl, String remark) {
        return jdbcTemplate.update(
                """
                UPDATE alb_route SET env=?, host=?, path_pattern=?, priority=?, upstream_id=?, enabled=?,
                strip_prefix=?, redirect_url=?, remark=?, gmt_modified=CURRENT_TIMESTAMP WHERE id=?
                """,
                env, host, pathPattern, priority, upstreamId, enabled ? 1 : 0, stripPrefix ? 1 : 0, redirectUrl, remark, id);
    }

    public int deleteRoute(Long id) {
        return jdbcTemplate.update("DELETE FROM alb_route WHERE system_locked = 0 AND id = ?", id);
    }

    public List<HeaderPolicy> listHeaderPolicies(Long routeId) {
        return jdbcTemplate.query(
                "SELECT * FROM alb_header_policy WHERE route_id = ? ORDER BY sort_order, id",
                HEADER_MAPPER,
                routeId);
    }

    public long insertHeaderPolicy(Long routeId, String direction, String op, String headerKey,
                                   String headerValue, int sortOrder, boolean enabled) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO alb_header_policy(route_id, direction, op, header_key, header_value, sort_order, enabled)
                    VALUES (?,?,?,?,?,?,?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, routeId);
            ps.setString(2, direction);
            ps.setString(3, op);
            ps.setString(4, headerKey);
            ps.setString(5, headerValue);
            ps.setInt(6, sortOrder);
            ps.setInt(7, enabled ? 1 : 0);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public int updateHeaderPolicy(Long id, String direction, String op, String headerKey,
                                  String headerValue, int sortOrder, boolean enabled) {
        return jdbcTemplate.update(
                """
                UPDATE alb_header_policy SET direction=?, op=?, header_key=?, header_value=?, sort_order=?, enabled=?,
                gmt_modified=CURRENT_TIMESTAMP WHERE id=?
                """,
                direction, op, headerKey, headerValue, sortOrder, enabled ? 1 : 0, id);
    }

    public int deleteHeaderPolicy(Long id) {
        return jdbcTemplate.update("DELETE FROM alb_header_policy WHERE id = ?", id);
    }

    public List<RoutingSnapshot.SnapshotRoute> listEnabledSnapshotRoutes() {
        return jdbcTemplate.query(
                """
                SELECT r.id, r.host, r.path_pattern, r.priority, u.target_url, u.websocket_enabled,
                       r.strip_prefix, r.redirect_url
                FROM alb_route r
                JOIN alb_upstream u ON u.id = r.upstream_id
                WHERE r.enabled = 1
                ORDER BY r.host, LENGTH(r.path_pattern) DESC, r.priority DESC, r.id
                """,
                (rs, rowNum) -> {
                    Long routeId = rs.getLong("id");
                    return new RoutingSnapshot.SnapshotRoute(
                            routeId,
                            rs.getString("host"),
                            rs.getString("path_pattern"),
                            rs.getInt("priority"),
                            rs.getString("target_url"),
                            rs.getInt("websocket_enabled") == 1,
                            rs.getInt("strip_prefix") == 1,
                            rs.getString("redirect_url"),
                            listEnabledHeaderPolicies(routeId)
                    );
                });
    }

    private List<RoutingSnapshot.SnapshotHeaderPolicy> listEnabledHeaderPolicies(Long routeId) {
        return jdbcTemplate.query(
                """
                SELECT direction, op, header_key, header_value, sort_order
                FROM alb_header_policy
                WHERE route_id = ? AND enabled = 1
                ORDER BY sort_order, id
                """,
                (rs, rowNum) -> new RoutingSnapshot.SnapshotHeaderPolicy(
                        rs.getString("direction"),
                        rs.getString("op"),
                        rs.getString("header_key"),
                        rs.getString("header_value"),
                        rs.getInt("sort_order")
                ),
                routeId);
    }

    public int nextReleaseVersion() {
        Integer max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version), 0) FROM alb_route_release",
                Integer.class);
        return (max == null ? 0 : max) + 1;
    }

    public void insertRelease(int version, String snapshotJson, String caddyConfig) {
        jdbcTemplate.update(
                "INSERT INTO alb_route_release(version, snapshot_json, caddy_config) VALUES (?,?,?)",
                version, snapshotJson, caddyConfig);
    }

    public Optional<RoutingSnapshot> findLatestSnapshot(int dataplanePort) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(version) FROM alb_route_release",
                Integer.class);
        if (version == null || version == 0) {
            return Optional.empty();
        }
        return Optional.of(new RoutingSnapshot(version, dataplanePort, listEnabledSnapshotRoutes()));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}

package com.yeswater.bids.sql.dialect;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.feature.Feature;

import java.util.function.Consumer;

/**
 * 按 {@link SqlDialectType} 配置 JSQLParser，使校验与目标库语法一致。
 */
public final class SqlDialectParserSupport {

    private SqlDialectParserSupport() {
    }

    /**
     * 返回用于 {@link net.sf.jsqlparser.parser.CCJSqlParserUtil#parseStatements(String, Consumer)} 的 parser 配置。
     */
    public static Consumer<CCJSqlParser> parserConfigurer(SqlDialectType dialect) {
        return switch (dialect) {
            case MYSQL -> parser -> {
            };
            case POSTGRESQL, OPENGAUSS -> parser -> parser.withFeature(Feature.allowPostgresSpecificSyntax, true);
        };
    }

    /**
     * 将内层查询包一层并追加行数上限占位参数（命名参数），方言一致时实现可共用。
     */
    public static String wrapSelectWithRowCap(String innerSelectSql, String namedLimitParameter) {
        return "select * from (" + innerSelectSql + ") bids_result limit :" + namedLimitParameter;
    }

    /**
     * 在内层查询外包一层并追加 LIMIT / OFFSET 命名参数（MySQL / PostgreSQL / openGauss 兼容写法）。
     */
    public static String wrapSelectWithPaging(String innerSelectSql, String limitNamedParameter, String offsetNamedParameter) {
        return "select * from ("
                + innerSelectSql
                + ") bids_result limit :"
                + limitNamedParameter
                + " offset :"
                + offsetNamedParameter;
    }
}

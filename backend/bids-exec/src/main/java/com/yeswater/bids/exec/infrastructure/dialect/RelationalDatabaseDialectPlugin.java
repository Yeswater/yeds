package com.yeswater.bids.exec.infrastructure.dialect;

import com.yeswater.bids.sql.dialect.SqlDialectType;
import net.sf.jsqlparser.parser.CCJSqlParser;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 关系型数据库方言插件：封装解析器配置与运行态查询改写（如行数上限包装）。
 */
public interface RelationalDatabaseDialectPlugin {

    /**
     * 返回本插件负责的方言类型集合（每种方言只能注册在一个插件中）。
     */
    Set<SqlDialectType> dialectTypes();

    /**
     * 返回 JSQLParser 解析前对 parser 的配置（Consumer）。
     */
    Consumer<CCJSqlParser> parserConfigurer();

    /**
     * 在内层 select 外包一层并追加命名 limit 参数。
     *
     * @param innerSelectSql      内层 select 文本
     * @param namedLimitParameter 命名参数名（不含冒号）
     */
    String wrapSelectWithRowCap(String innerSelectSql, String namedLimitParameter);
}

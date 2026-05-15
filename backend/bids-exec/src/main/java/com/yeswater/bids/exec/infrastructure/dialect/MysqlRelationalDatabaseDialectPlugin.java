package com.yeswater.bids.exec.infrastructure.dialect;

import com.yeswater.bids.sql.dialect.SqlDialectParserSupport;
import com.yeswater.bids.sql.dialect.SqlDialectType;
import net.sf.jsqlparser.parser.CCJSqlParser;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class MysqlRelationalDatabaseDialectPlugin implements RelationalDatabaseDialectPlugin {

    @Override
    public Set<SqlDialectType> dialectTypes() {
        return EnumSet.of(SqlDialectType.MYSQL);
    }

    @Override
    public Consumer<CCJSqlParser> parserConfigurer() {
        return SqlDialectParserSupport.parserConfigurer(SqlDialectType.MYSQL);
    }

    @Override
    public String wrapSelectWithRowCap(String innerSelectSql, String namedLimitParameter) {
        return SqlDialectParserSupport.wrapSelectWithRowCap(innerSelectSql, namedLimitParameter);
    }

    @Override
    public String wrapSelectWithPaging(String innerSelectSql, String limitNamedParameter, String offsetNamedParameter) {
        return SqlDialectParserSupport.wrapSelectWithPaging(innerSelectSql, limitNamedParameter, offsetNamedParameter);
    }
}

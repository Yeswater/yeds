package com.yeswater.bids.exec.infrastructure.dialect;

import com.yeswater.bids.sql.dialect.SqlDialectParserSupport;
import com.yeswater.bids.sql.dialect.SqlDialectType;
import net.sf.jsqlparser.parser.CCJSqlParser;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class PostgresFamilyRelationalDatabaseDialectPlugin implements RelationalDatabaseDialectPlugin {

    @Override
    public Set<SqlDialectType> dialectTypes() {
        return EnumSet.of(SqlDialectType.POSTGRESQL, SqlDialectType.OPENGAUSS);
    }

    @Override
    public Consumer<CCJSqlParser> parserConfigurer() {
        return SqlDialectParserSupport.parserConfigurer(SqlDialectType.POSTGRESQL);
    }

    @Override
    public String wrapSelectWithRowCap(String innerSelectSql, String namedLimitParameter) {
        return SqlDialectParserSupport.wrapSelectWithRowCap(innerSelectSql, namedLimitParameter);
    }
}

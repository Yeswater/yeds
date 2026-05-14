package com.yeswater.bids.exec.application;

import com.yeswater.bids.exec.infrastructure.web.ApiException;
import com.yeswater.bids.sql.dialect.SqlDialectParserSupport;
import com.yeswater.bids.sql.dialect.SqlDialectType;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SqlSafetyValidator {

    public void validateReadonlySelect(String sql) {
        validateReadonlySelect(sql, SqlDialectType.MYSQL);
    }

    /**
     * 按数据源方言校验 SQL 是否为单条只读 select。
     */
    public void validateReadonlySelect(String sql, SqlDialectType dialect) {
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(
                    normalizeNamedParameters(sql),
                    SqlDialectParserSupport.parserConfigurer(dialect)
            );
            if (statements.getStatements().size() != 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SQL 只能包含一条语句");
            }
            Statement statement = statements.getStatements().getFirst();
            if (!(statement instanceof Select)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SQL 只允许执行 select");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SQL 解析失败：" + exception.getMessage());
        }
    }

    private String normalizeNamedParameters(String sql) {
        return sql.replaceAll("(?<!:):[A-Za-z][A-Za-z0-9_]*", "null");
    }
}

package com.xie.platform.config;

import com.xie.platform.model.enumValue.SecurityLevel;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：SecurityLevel 枚举 <-> 数据库 INT (level)
 */
@MappedTypes(SecurityLevel.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class SecurityLevelTypeHandler extends BaseTypeHandler<SecurityLevel> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, SecurityLevel parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getLevel());
    }

    @Override
    public SecurityLevel getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int level = rs.getInt(columnName);
        return rs.wasNull() ? null : SecurityLevel.fromLevel(level);
    }

    @Override
    public SecurityLevel getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int level = rs.getInt(columnIndex);
        return rs.wasNull() ? null : SecurityLevel.fromLevel(level);
    }

    @Override
    public SecurityLevel getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int level = cs.getInt(columnIndex);
        return cs.wasNull() ? null : SecurityLevel.fromLevel(level);
    }
}

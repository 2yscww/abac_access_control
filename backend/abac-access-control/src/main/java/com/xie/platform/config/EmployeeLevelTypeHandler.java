package com.xie.platform.config;

import com.xie.platform.model.enumValue.EmployeeLevel;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：EmployeeLevel 枚举 <-> 数据库 INT (rank)
 */
@MappedTypes(EmployeeLevel.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class EmployeeLevelTypeHandler extends BaseTypeHandler<EmployeeLevel> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, EmployeeLevel parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getRank());
    }

    @Override
    public EmployeeLevel getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int rank = rs.getInt(columnName);
        return rs.wasNull() ? null : EmployeeLevel.fromRank(rank);
    }

    @Override
    public EmployeeLevel getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int rank = rs.getInt(columnIndex);
        return rs.wasNull() ? null : EmployeeLevel.fromRank(rank);
    }

    @Override
    public EmployeeLevel getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int rank = cs.getInt(columnIndex);
        return cs.wasNull() ? null : EmployeeLevel.fromRank(rank);
    }
}

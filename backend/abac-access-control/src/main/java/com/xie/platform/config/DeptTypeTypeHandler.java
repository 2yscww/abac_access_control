package com.xie.platform.config;

import com.xie.platform.model.enumValue.DeptType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：DeptType 枚举 <-> 数据库 VARCHAR (dept_type)
 * 数据库存枚举的 name() 字符串，读取时转回枚举。
 */
@MappedTypes(DeptType.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class DeptTypeTypeHandler extends BaseTypeHandler<DeptType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, DeptType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public DeptType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String val = rs.getString(columnName);
        return val == null ? null : DeptType.fromName(val);
    }

    @Override
    public DeptType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String val = rs.getString(columnIndex);
        return val == null ? null : DeptType.fromName(val);
    }

    @Override
    public DeptType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String val = cs.getString(columnIndex);
        return val == null ? null : DeptType.fromName(val);
    }
}

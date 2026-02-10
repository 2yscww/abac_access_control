package com.xie.platform.config;

import com.xie.platform.model.enumValue.ProjectPhase;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：ProjectPhase 枚举 <-> 数据库 INT (code)
 */
@MappedTypes(ProjectPhase.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class ProjectPhaseTypeHandler extends BaseTypeHandler<ProjectPhase> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ProjectPhase parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public ProjectPhase getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : ProjectPhase.fromCode(code);
    }

    @Override
    public ProjectPhase getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : ProjectPhase.fromCode(code);
    }

    @Override
    public ProjectPhase getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : ProjectPhase.fromCode(code);
    }
}

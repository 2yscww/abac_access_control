package com.xie.platform.config;

import com.xie.platform.model.enumValue.AssetType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：AssetType 枚举 <-> 数据库 VARCHAR (存储枚举名称)
 *
 * 注意：数据库字段是 VARCHAR，但为了保持一致性，我们也可以改为存储 code（INT）
 * 当前实现：存储枚举名称（REQUIREMENT_DOC, DESIGN_DOC 等）
 */
@MappedTypes(AssetType.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class AssetTypeTypeHandler extends BaseTypeHandler<AssetType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AssetType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public AssetType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String name = rs.getString(columnName);
        return rs.wasNull() ? null : AssetType.valueOf(name);
    }

    @Override
    public AssetType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String name = rs.getString(columnIndex);
        return rs.wasNull() ? null : AssetType.valueOf(name);
    }

    @Override
    public AssetType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String name = cs.getString(columnIndex);
        return cs.wasNull() ? null : AssetType.valueOf(name);
    }
}

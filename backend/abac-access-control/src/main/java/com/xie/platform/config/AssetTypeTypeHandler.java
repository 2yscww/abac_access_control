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
 * MyBatis TypeHandler：AssetType 枚举 <-> 数据库 INT (code)
 */
@MappedTypes(AssetType.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class AssetTypeTypeHandler extends BaseTypeHandler<AssetType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AssetType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public AssetType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : AssetType.fromCode(code);
    }

    @Override
    public AssetType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : AssetType.fromCode(code);
    }

    @Override
    public AssetType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : AssetType.fromCode(code);
    }
}

package com.xie.platform.config;

import com.xie.platform.model.enumValue.AssetType;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetTypeTypeHandlerTest {

    private final AssetTypeTypeHandler handler = new AssetTypeTypeHandler();

    @Test
    void setNonNullParameter_shouldPersistAssetTypeCode() throws Exception {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        handler.setNonNullParameter(preparedStatement, 1, AssetType.REQUIREMENT_DOC, JdbcType.INTEGER);

        verify(preparedStatement).setInt(1, AssetType.REQUIREMENT_DOC.getCode());
    }

    @Test
    void getNullableResultByColumnName_shouldConvertCodeToEnum() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("assets_type")).thenReturn(AssetType.DESIGN_DOC.getCode());
        when(resultSet.wasNull()).thenReturn(false);

        AssetType result = handler.getNullableResult(resultSet, "assets_type");

        assertEquals(AssetType.DESIGN_DOC, result);
    }

    @Test
    void getNullableResultByColumnIndex_shouldReturnNullWhenDatabaseValueIsNull() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt(1)).thenReturn(0);
        when(resultSet.wasNull()).thenReturn(true);

        AssetType result = handler.getNullableResult(resultSet, 1);

        assertNull(result);
    }

    @Test
    void getNullableResultFromCallableStatement_shouldConvertCodeToEnum() throws Exception {
        CallableStatement callableStatement = mock(CallableStatement.class);
        when(callableStatement.getInt(1)).thenReturn(AssetType.OPS_DOC.getCode());
        when(callableStatement.wasNull()).thenReturn(false);

        AssetType result = handler.getNullableResult(callableStatement, 1);

        assertEquals(AssetType.OPS_DOC, result);
    }
}

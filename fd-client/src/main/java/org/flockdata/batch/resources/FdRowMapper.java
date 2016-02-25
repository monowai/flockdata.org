package org.flockdata.batch.resources;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nabil on 18/01/2016.
 */
public class FdRowMapper implements RowMapper<Map<String,Object>>{
    @Override
    public Map<String,Object> mapRow(ResultSet resultSet, int i) throws SQLException {
        Map<String,Object> resultMap = new HashMap<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        Integer columnCount = metaData.getColumnCount();
        for (int j = 0; j < columnCount; j++) {
            resultMap.put(metaData.getColumnName(j + 1),resultSet.getObject(j+1));
        }
        return resultMap;
    }
}

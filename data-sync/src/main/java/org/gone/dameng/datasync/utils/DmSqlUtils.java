package org.gone.dameng.datasync.utils;

import dm.jdbc.driver.DmdbConnection;
import lombok.extern.slf4j.Slf4j;
import org.gone.dameng.datasync.model.di.req.DatabaseInstanceBaseParam;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.function.Function;

@Slf4j
public class DmSqlUtils {

    public static <T> T executeSql(DatabaseInstanceBaseParam param, Function<Connection, T> callback) {
        try {
            String jdbcUrl = String.format("jdbc:dm://%s:%s", param.getDatabaseHost(), param.getDatabasePort());
            Class.forName("dm.jdbc.driver.DmDriver");  //加载驱动程序
            try (DmdbConnection connection = (DmdbConnection) DriverManager.getConnection(jdbcUrl, param.getDatabaseUsername(), param.getDatabasePassword())) {
                return callback.apply(connection);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}


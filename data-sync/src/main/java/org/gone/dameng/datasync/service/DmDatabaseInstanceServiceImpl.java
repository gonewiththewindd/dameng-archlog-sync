package org.gone.dameng.datasync.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gone.dameng.datasync.constants.SystemScriptNameConstants;
import org.gone.dameng.datasync.constants.SystemScriptTemplates;
import org.gone.dameng.datasync.model.di.DatabaseInstanceRuntimeInfo;
import org.gone.dameng.datasync.model.di.req.DatabaseInstanceBaseParam;
import org.gone.dameng.datasync.model.di.resp.DBOperationResp;
import org.gone.dameng.datasync.service.def.DatabaseInstanceService;
import org.gone.dameng.datasync.utils.DmSqlUtils;
import org.gone.dameng.datasync.utils.SshUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DmDatabaseInstanceServiceImpl implements DatabaseInstanceService {

    @Override
    public Boolean isUp(DatabaseInstanceBaseParam param) {

        String cmd = SystemScriptTemplates.getScript(
                param.getSystemType(),
                SystemScriptNameConstants.PORT_LISTENING,
                param.getDatabasePort()
        );
        String result = SshUtils.execRemoteCommand(
                param.getSshHost(),
                param.getSshPort(),
                param.getSshUsername(),
                param.getSshPassword(),
                cmd,
                10
        );
        return StringUtils.isNoneBlank(result);
    }

    @Override
    public Boolean start(DatabaseInstanceBaseParam param) {

        String cmd = SystemScriptTemplates.getScript(
                param.getSystemType(),
                SystemScriptNameConstants.SERVICE_START,
                param.getServiceName()
        );
        SshUtils.execRemoteCommand(
                param.getSshHost(),
                param.getSshPort(),
                param.getSshUsername(),
                param.getSshPassword(),
                cmd,
                10
        );
        try {
            TimeUnit.MILLISECONDS.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return this.isUp(param);
    }

    @Override
    public Boolean shutdown(DatabaseInstanceBaseParam param) {

        String cmd = SystemScriptTemplates.getScript(
                param.getSystemType(),
                SystemScriptNameConstants.SERVICE_STOP,
                param.getServiceName()
        );
        SshUtils.execRemoteCommand(
                param.getSshHost(),
                param.getSshPort(),
                param.getSshUsername(),
                param.getSshPassword(),
                cmd,
                10
        );
        try {
            TimeUnit.MILLISECONDS.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return !this.isUp(param);
    }

    @Override
    public Boolean alterStatus(DatabaseInstanceBaseParam param) {
        return null;
    }

    @Override
    public Boolean alterModeAndStatus(DatabaseInstanceBaseParam param) {

        DatabaseInstanceRuntimeInfo runtimeInformation = this.runtimeInformation(param);
        if (param.getMode().equalsIgnoreCase(runtimeInformation.getMode())) {
            // already at target mode
            return true;
        }
        List<String> sqlList = new ArrayList<>();
        if (!"MOUNT".equalsIgnoreCase(runtimeInformation.getStatus())) {
            sqlList.add("ALTER DATABASE MOUNT;");
        }
        sqlList.add(String.format("ALTER DATABASE %s;", param.getMode()));
        sqlList.add(String.format("ALTER DATABASE %s;", param.getStatus()));

        log.info("alter mode and status sql:{}", sqlList.toArray().toString());

        return DmSqlUtils.executeSql(param, connection -> {
            try (Statement statement = connection.createStatement()) {
                for (String sql : sqlList) {
                    statement.addBatch(sql);
                }
                statement.executeBatch();
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public DatabaseInstanceRuntimeInfo runtimeInformation(DatabaseInstanceBaseParam param) {
        return DmSqlUtils.executeSql(param, connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement("select * from v$instance;")) {
                preparedStatement.execute();

                ResultSet resultSet = preparedStatement.getResultSet();
                if (resultSet.next()) {
                    return extractAsRuntimeInfo(resultSet);
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private DatabaseInstanceRuntimeInfo extractAsRuntimeInfo(ResultSet resultSet) throws SQLException {

        String name = resultSet.getString("NAME");
        String instanceName = resultSet.getString("INSTANCE_NAME");
        String instanceNumber = resultSet.getString("INSTANCE_NUMBER");
        String hostName = resultSet.getString("HOST_NAME");
        String svrVersion = resultSet.getString("SVR_VERSION");
        String dbVersion = resultSet.getString("db_version");
        String startTime = resultSet.getString("START_TIME");
        String status = resultSet.getString("STATUS$");
        String mode = resultSet.getString("MODE$");
        String oguid = resultSet.getString("OGUID");
        String dscSeqno = resultSet.getString("DSC_SEQNO");
        String dscRole = resultSet.getString("DSC_ROLE");
        String buildVersion = resultSet.getString("BUILD_VERSION");
        String buildTime = resultSet.getString("BUILD_TIME");

        return new DatabaseInstanceRuntimeInfo()
                .setName(name)
                .setInstanceName(instanceName)
                .setInstanceNumber(instanceNumber)
                .setHostName(hostName)
                .setSvrVersion(svrVersion)
                .setDbVersion(dbVersion)
                .setStartTime(startTime)
                .setStatus(status)
                .setMode(mode)
                .setOguid(oguid)
                .setDscSeqno(dscSeqno)
                .setDscRole(dscRole)
                .setBuildVersion(buildVersion)
                .setBuildTime(buildTime);
    }

    private DBOperationResp<DatabaseInstanceRuntimeInfo> parseAsRuntimeInfo(String result) {
        return null;
    }
}

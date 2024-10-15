package org.gone.dameng.datasync.model.di;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DatabaseInstance {

    String sshHost;
    int sshPort;
    String sshUsername;
    String sshPassword;
    String databaseUsername;
    String databasePassword;
    String databaseHost;
    String databasePort;
    String archLogFilePath;
    String archLogSyncReplayPath;
    String serviceName;
    String instanceName;

}

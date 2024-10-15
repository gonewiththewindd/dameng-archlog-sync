package org.gone.dameng.datasync.model.di.req;

import lombok.Data;
import lombok.experimental.Accessors;
import org.gone.dameng.datasync.enums.SystemTypeEnums;

@Data
@Accessors(chain = true)
public class DatabaseInstanceBaseParam {

    protected SystemTypeEnums systemType;

    protected String sshHost;
    protected int sshPort;
    protected String sshUsername;
    protected String sshPassword;

    protected String databaseHost;
    protected int databasePort;
    protected String databaseUsername;
    protected String databasePassword;

    protected String status;
    protected String mode;
    protected String serviceName;

    protected boolean justUpDown;
}

package org.gone.dameng.datasync.model.di;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DatabaseInstanceRuntimeInfo {

    private String name;
    private String instanceName;
    private String instanceNumber;
    private String hostName;
    private String svrVersion;
    private String dbVersion;
    private String startTime;
    private String status;
    private String mode;
    private String oguid;
    private String dscSeqno;
    private String dscRole;
    private String buildVersion;
    private String buildTime;

}

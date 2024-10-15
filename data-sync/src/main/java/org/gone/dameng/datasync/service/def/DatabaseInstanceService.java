package org.gone.dameng.datasync.service.def;

import org.gone.dameng.datasync.model.di.DatabaseInstanceRuntimeInfo;
import org.gone.dameng.datasync.model.di.req.DatabaseInstanceBaseParam;

public interface DatabaseInstanceService {

    Boolean isUpDown(DatabaseInstanceBaseParam param);

    Boolean up(DatabaseInstanceBaseParam param);

    Boolean shutdown(DatabaseInstanceBaseParam param);

    Boolean alterStatus(DatabaseInstanceBaseParam param);

    Boolean alterModeAndStatus(DatabaseInstanceBaseParam param);

    DatabaseInstanceRuntimeInfo runtimeInformation(DatabaseInstanceBaseParam param);
}

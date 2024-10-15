package org.gone.dameng.datasync.enums.script;

import lombok.Getter;
import org.gone.dameng.datasync.constants.SystemScriptNameConstants;
import org.gone.dameng.datasync.enums.SystemTypeEnums;

@Getter
public enum WindowsScriptEnums {

    /**
     * netstat -an | findstr ${port} | findstr LISTENING
     **/
    PORT_LISTENING(SystemScriptNameConstants.PORT_LISTENING, "netstat -an | findstr %s | findstr LISTENING"),
    /**
     * sc stop ${serviceName}
     **/
    SERVICE_STOP(SystemScriptNameConstants.SERVICE_STOP, "sc stop %s"),
    /**
     * sc start ${serviceName}
     **/
    SERVICE_START(SystemScriptNameConstants.SERVICE_START, "sc start %s"),
    /**
     * disql ${username}/${password}@${host}:${port} -e "select mode$ from v$instance"
     **/
    DM_DATABASE_INSTANCE_RUNTIME_INFO(
            SystemScriptNameConstants.DM_DATABASE_INSTANCE_RUNTIME_INFO,
            "disql %s/%s@%s:%s -e \"select * from v$instance\""),
    ;

    private String scriptName;
    private String template;

    WindowsScriptEnums(String scriptName, String template) {
        this.scriptName = scriptName;
        this.template = template;
    }

    public String key() {
        return SystemTypeEnums.WINDOWS.toString().concat("_").concat(scriptName);
    }

    public String format(Object... args) {
        return String.format(this.template, args);
    }
}

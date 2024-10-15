package org.gone.dameng.datasync.enums.script;

import lombok.Getter;
import org.gone.dameng.datasync.constants.SystemScriptNameConstants;
import org.gone.dameng.datasync.enums.SystemTypeEnums;

@Getter
public enum LinuxScriptEnums {

    /** ss -l | grep ${port}    **/
    PORT_LISTENING(SystemScriptNameConstants.PORT_LISTENING, "ss -l | grep %s"),
    /** disql ${username}/${password}@${host}:${port} -e "select mode$ from v$instance" **/
    DM_DATABASE_INSTANCE_RUNTIME_INFO(
            SystemScriptNameConstants.DM_DATABASE_INSTANCE_RUNTIME_INFO,
            "disql %s/%s@%s:%s -e \"select * from v$instance\""),
    ;

    private String scriptName;
    private String template;

    LinuxScriptEnums(String scriptName, String template) {
        this.scriptName = scriptName;
        this.template = template;
    }

    public String key(){
        return SystemTypeEnums.LINUX.toString().concat("_").concat(scriptName);
    }

    public String format(Object... args) {
        return String.format(this.template, args);
    }
}

package org.gone.dameng.datasync.constants;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.lang3.StringUtils;
import org.gone.dameng.datasync.enums.SystemTypeEnums;
import org.gone.dameng.datasync.enums.script.LinuxScriptEnums;
import org.gone.dameng.datasync.enums.script.WindowsScriptEnums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SystemScriptTemplates {

    private static final Map<String, String> TEMPLATES = new HashMap<>() {{

        Map<String, String> windowsScript = Arrays.stream(WindowsScriptEnums.values())
                .map(e -> new DefaultKeyValue<>(e.key(), e.getTemplate()))
                .collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue));
        Map<String, String> linuxScript = Arrays.stream(LinuxScriptEnums.values())
                .map(e -> new DefaultKeyValue<>(e.key(), e.getTemplate()))
                .collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue));
        putAll(windowsScript);
    }};

    public static String getScript(SystemTypeEnums systemType, String scriptName, Object... args) {
        String key = systemType.toString().concat("_").concat(scriptName);
        String template = TEMPLATES.get(key);
        if (StringUtils.isNotBlank(template)) {
            return String.format(template, args);
        }
        throw new IllegalArgumentException(String.format("script not exist:%s", key));
    }

}

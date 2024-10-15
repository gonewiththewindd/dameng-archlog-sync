package org.gone.dameng.datasync.enums;

public enum RedisKeyEnums {

    REPLAY_PENDING_LIST("replay:pending_list"),
    REPLAY_LAST_SCN("replay:last_scn"),
    ;

    private String format;

    RedisKeyEnums(String format) {
        this.format = format;
    }

    public String format(Object... args) {
        return String.format(format, args);
    }
}

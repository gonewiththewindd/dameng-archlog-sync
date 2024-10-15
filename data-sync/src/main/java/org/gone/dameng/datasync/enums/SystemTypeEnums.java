package org.gone.dameng.datasync.enums;

import lombok.Getter;

@Getter
public enum SystemTypeEnums {

    WINDOWS(0),
    LINUX(1),
    ;

    private int value;

    SystemTypeEnums(int value) {
        this.value = value;
    }
}

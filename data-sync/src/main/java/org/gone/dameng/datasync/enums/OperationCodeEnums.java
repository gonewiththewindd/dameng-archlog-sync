package org.gone.dameng.datasync.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum OperationCodeEnums {

    INSERT(1),
    DELETE(2),
    UPDATE(3),
    DDL(5),
    START(6),
    COMMIT(7),
    ROLLBACK(36),
    ;

    Integer code;

    OperationCodeEnums(int code) {
        this.code = code;
    }

    public static OperationCodeEnums instanceOf(Integer code) {
        return Arrays.stream(values()).filter(e -> e.code.equals(code)).findFirst().orElseThrow(() -> new IllegalArgumentException(""));
    }
}

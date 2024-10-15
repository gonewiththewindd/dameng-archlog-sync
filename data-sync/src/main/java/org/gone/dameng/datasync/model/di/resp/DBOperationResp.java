package org.gone.dameng.datasync.model.di.resp;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DBOperationResp<T> {

    protected boolean succeed;
    protected T data;

    public static <T> DBOperationResp succeed(T data) {
        return new DBOperationResp().setSucceed(true).setData(data);
    }

    public static <T> DBOperationResp succeed() {
        return new DBOperationResp().setSucceed(true);
    }

    public static DBOperationResp failed() {
        return new DBOperationResp().setSucceed(false);
    }
}

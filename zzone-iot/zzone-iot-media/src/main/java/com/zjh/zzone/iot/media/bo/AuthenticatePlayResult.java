package com.zjh.zzone.iot.media.bo;

import lombok.Data;

/**
 * 播放鉴权结果实体
 *
 * @author zjh
 * @date 2025-08-11 9:45
 */
@Data
public class AuthenticatePlayResult {

    private boolean result;

    private String message;

    public static AuthenticatePlayResult ok(){
        AuthenticatePlayResult result = new AuthenticatePlayResult();
        result.setResult(true);
        return result;
    }

    public static AuthenticatePlayResult fail(String message){
        AuthenticatePlayResult result = new AuthenticatePlayResult();
        result.setResult(false);
        result.setMessage(message);
        return result;
    }

}

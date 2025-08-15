package com.zjh.zzone.iot.media.zlm.hook.params;

/**
 * zlm hook事件中的on_server_keepalive事件的参数
 *
 * @author zjh
 * @since 2025-07-02 15:48
 */
public class OnServerKeepaliveHookParam extends HookParam {

    private ServerKeepaliveParam data;

    public ServerKeepaliveParam getData() {
        return data;
    }

    public void setData(ServerKeepaliveParam data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "OnServerKeepaliveHookParam{" +
                "data=" + data +
                '}';
    }
}

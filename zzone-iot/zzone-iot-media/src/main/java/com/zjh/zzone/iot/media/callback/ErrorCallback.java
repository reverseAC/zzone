package com.zjh.zzone.iot.media.callback;

public interface ErrorCallback<T> {

    void run(int code, String msg, T data);
}

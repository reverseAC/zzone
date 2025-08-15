package com.zjh.zzone.iot.media.service;

import com.ylg.iot.entity.MediaDevice;

/**
 * 设备媒体信息
 *
 * @author zjh
 * @since 2025-03-28 10:14
 */
public interface PtzService {

    /**
     * 前端设备控制
     *
     * @param device 设备信息
     * @param channelGbId 通道国标编号
     * @param cmdCode 指令码
     * @param parameter1 数据一
     * @param parameter2 数据二
     * @param combindCode2 组合码二
     */
    void frontEndCommand(MediaDevice device, String channelGbId, int cmdCode, int parameter1, int parameter2, int combindCode2);

}

package com.zjh.zzone.iot.media.service.impl;

import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.gb28181.transmit.cmd.SIPCommander;
import com.ylg.iot.media.service.PtzService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

@Slf4j
@Service
public class PtzServiceImpl implements PtzService {

    @Autowired
    private SIPCommander cmder;

    @Override
    public void frontEndCommand(MediaDevice device, String channelId, int cmdCode, int parameter1, int parameter2, int combindCode2) {
        // 判断设备是否属于当前平台, 如果不属于则发起自动调用
        try {
            cmder.frontEndCmd(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 前端控制: {}", e.getMessage());
            throw new RuntimeException("命令发送失败: " + e.getMessage());
        }
    }
}

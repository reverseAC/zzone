package com.zjh.zzone.iot.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.entity.CloudRecord;
import com.ylg.iot.media.dto.CloudRecordDTO;
import com.ylg.iot.vo.DownloadFileInfo;

import java.util.List;
import java.util.Set;

/**
 * 云端录像管理
 *
 * @author zjh
 */
public interface CloudRecordService extends IService<CloudRecord> {

    /**
     * 获取播放地址
     */
    DownloadFileInfo getPlayUrlPath(Long recordId);

    /**
     * 查询录像信息列表
     *
     * @param cloudRecord 查询条件
     * @return 云端录像信息
     */
    List<CloudRecord> getAllList(CloudRecordDTO cloudRecord);

    /**
     * 加载录像文件，形成录像流
     *
     * @param app 应用名称
     * @param stream 流名称
     * @param date 日期
     * @param callback 回调
     */
    void loadRecord(String app, String stream, String date, ErrorCallback<StreamDetail> callback);

    /**
     * 批量删除云端录像
     *
     * @param ids 云端录像id集合
     */
    void deleteFileByIds(Set<Long> ids);

}

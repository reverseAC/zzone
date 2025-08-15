package com.zjh.zzone.iot.media.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ylg.iot.entity.RecordShareLog;
import com.ylg.iot.media.dto.RecordShareLogDTO;
import com.ylg.iot.media.vo.RecordShareLogVO;

/**
 * 录像分享记录管理
 *
 * @author zjh
 * @date 2025-07-24 14:33
 */
public interface RecordShareLogService extends IService<RecordShareLog> {

    /**
     * 分页查询分享记录
     *
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<RecordShareLogVO> shareLogPage(IPage<RecordShareLogVO> page, RecordShareLogDTO params);

    /**
     * 根据id查询分享记录详情
     *
     * @param id 分享id
     * @return 分页结果
     */
    RecordShareLogVO getShareLogById(Long id);


    /**
     * 创建录像分享
     *
     * @param params 参数
     * @return 分享记录id
     */
	Long createShare(RecordShareLogDTO params);

    /**
     * 更新分享记录
     *
     * @param params 参数
     */
    void updateShare(RecordShareLogDTO params);

    /**
     * 失效分享记录
     *
     * @param logId 记录id
     */
    void expireShare(Long logId);

}



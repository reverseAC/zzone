package com.zjh.zzone.iot.media.mapper;

import com.ylg.iot.entity.CloudRecord;
import com.ylg.iot.media.dto.CloudRecordDTO;
import com.ylg.mybatis.base.YlgBaseMapper;

import java.util.List;

@Mapper
public interface CloudRecordMapper extends YlgBaseMapper<CloudRecord> {

    /**
     * 自定义查询列表
     *
     * @param cloudRecord 查询条件
     * @return 云端录像信息列表
     */
    List<CloudRecord> getList(@Param("cloudRecord") CloudRecordDTO cloudRecord);

}

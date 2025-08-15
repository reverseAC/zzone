package com.zjh.zzone.iot.media.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ylg.iot.entity.RecordShareLog;
import com.ylg.iot.media.dto.RecordShareLogDTO;
import com.ylg.iot.media.vo.RecordShareLogVO;
import com.ylg.mybatis.base.YlgBaseMapper;

import java.util.List;
import java.util.Set;

@Mapper
public interface RecordShareLogMapper extends YlgBaseMapper<RecordShareLog> {

    /**
     * 自定义查询分页
     *
     * @param cloudRecord 查询条件
     * @return 云端录像信息列表
     */
    List<RecordShareLogVO> selectShareLogPage(@Param("page") IPage<RecordShareLogVO> page, @Param("recordShareLog") RecordShareLogDTO cloudRecord);


    /**
     * 获取过期的分享，返回相应的云端录像id
     * @return 云端录像id
     */
    Set<Long> getExpireShare();
}

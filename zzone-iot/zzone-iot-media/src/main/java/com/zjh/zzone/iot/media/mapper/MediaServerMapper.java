package com.zjh.zzone.iot.media.mapper;

import com.ylg.iot.entity.MediaServer;
import com.ylg.mybatis.base.YlgBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流媒体服务信息 Mapper接口
 *
 * @author zjh
 * @since 2025-06-28 10:19
 */
@Mapper
public interface MediaServerMapper extends YlgBaseMapper<MediaServer> {

    /**
     * 根据服务ID查询流媒体服务信息
     * @param serverId 服务ID
     * @return 流媒体服务信息
     */
    MediaServer selectByServerId(@Param("serverId") String serverId);
}

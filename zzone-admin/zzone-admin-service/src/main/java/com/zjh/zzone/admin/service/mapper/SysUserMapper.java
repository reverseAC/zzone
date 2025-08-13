package com.zjh.zzone.admin.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.zzone.admin.api.dto.UserDTO;
import com.zjh.zzone.admin.api.entity.SysUser;
import com.zjh.zzone.admin.api.vo.UserVO;
import org.apache.ibatis.annotations.Param;

/**
 * 用户信息Mapper 接口
 *
 * @author zjh
 * @date 2025-08-13 14:15
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户信息
     *
     * @param page 分页参数
     * @param userDTO 查询参数
     * @return 用户信息分页
     */
    IPage<UserVO> selectUsersByPage(Page page, @Param("user") UserDTO userDTO);


}

package com.zjh.zzone.admin.service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjh.zzone.admin.api.dto.UserDTO;
import com.zjh.zzone.admin.api.entity.SysUser;

/**
 * 用户管理服务 接口
 *
 * @author zjh
 * @date 2025-08-13 14:13
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 分页查询用户
     * @param page 分页参数
     * @param userDTO 查询条件
     * @return 用户信息分页
     */
    IPage getUsersByPage(Page page, UserDTO userDTO);

}

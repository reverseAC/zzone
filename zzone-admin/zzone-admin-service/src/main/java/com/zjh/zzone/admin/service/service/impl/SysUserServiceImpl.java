package com.zjh.zzone.admin.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.zzone.admin.api.dto.UserDTO;
import com.zjh.zzone.admin.api.entity.SysUser;
import com.zjh.zzone.admin.service.mapper.SysUserMapper;
import com.zjh.zzone.admin.service.service.SysUserService;

/**
 * 用户管理服务 实现类
 *
 * @author zjh
 * @date 2025-08-13 14:44
 */
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    @Override
    public IPage getUsersByPage(Page page, UserDTO userDTO) {
        return baseMapper.selectUsersByPage(page, userDTO);;
    }
}

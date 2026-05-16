package com.airesumeforge.auth.mapper;

import com.airesumeforge.auth.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * 继承BaseMapper获得基本的CRUD能力
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

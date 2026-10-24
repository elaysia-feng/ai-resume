package com.elias.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elias.chat.entity.UserSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户设置 Mapper
 */
@Mapper
public interface UserSettingMapper extends BaseMapper<UserSetting> {
}

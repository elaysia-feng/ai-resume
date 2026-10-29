package com.elias.mapper.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elias.entity.notification.VerificationCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {
}

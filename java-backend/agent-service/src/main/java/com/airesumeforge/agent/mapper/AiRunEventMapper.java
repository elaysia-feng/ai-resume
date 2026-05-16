package com.airesumeforge.agent.mapper;

import com.airesumeforge.agent.entity.AiRunEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent run 事件 Mapper
 */
@Mapper
public interface AiRunEventMapper extends BaseMapper<AiRunEvent> {
}

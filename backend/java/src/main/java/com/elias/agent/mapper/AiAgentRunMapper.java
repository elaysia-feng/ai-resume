package com.elias.agent.mapper;

import com.elias.agent.entity.AiAgentRun;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Agent 运行记录 Mapper。 */
@Mapper
public interface AiAgentRunMapper extends BaseMapper<AiAgentRun> {

    /**
     * 批量读取每个会话优先展示的 run：存在活跃 run 时返回最新活跃项，否则返回最近一项。
     * SQL 对每个会话执行一次带复合索引的 top-1 查找，不加载完整 run 历史。
     *
     * @param userId     当前用户标识
     * @param sessionIds 会话标识列表
     * @return 每个会话至多一条 run 摘要
     */
    List<AiAgentRun> selectPreferredSessionRuns(
            @Param("userId") Long userId,
            @Param("sessionIds") List<Long> sessionIds);
}

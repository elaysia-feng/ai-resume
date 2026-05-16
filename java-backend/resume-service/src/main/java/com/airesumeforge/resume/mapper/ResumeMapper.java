package com.airesumeforge.resume.mapper;

import com.airesumeforge.resume.entity.Resume;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历Mapper
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}

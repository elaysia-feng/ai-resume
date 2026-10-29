package com.elias.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elias.dto.chat.request.ChatWorkspaceRequest;
import com.elias.dto.chat.response.ChatWorkspaceResponse;
import com.elias.entity.chat.UserSetting;
import com.elias.mapper.chat.UserSettingMapper;
import com.elias.service.chat.WorkspaceService;
import com.elias.common.context.UserContext;
import com.elias.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工作区服务实现。
 *
 * <p>工作区根目录存在 user_setting 表的 WORKSPACE_ROOT 里，是用户级设置。</p>
 */
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    /**
     * 工作区根目录的配置项 key
     */
    public static final String WORKSPACE_ROOT_KEY = "WORKSPACE_ROOT";

    private final UserSettingMapper userSettingMapper;

    @Override
    public ChatWorkspaceResponse getWorkspace() {
        Long userId = UserContext.verifyGetUserId();
        return ChatWorkspaceResponse.builder().path(findWorkspaceRoot(userId)).build();
    }

    @Override
    @Transactional
    public ChatWorkspaceResponse setWorkspace(ChatWorkspaceRequest request) {
        Long userId = UserContext.verifyGetUserId();
        String path = request == null ? null : request.getPath();
        return ChatWorkspaceResponse.builder().path(saveWorkspaceRoot(userId, path)).build();
    }

    @Override
    public String resolveWorkspaceRoot(Long userId, String requestedPath) {
        if (requestedPath != null && !requestedPath.isBlank()) {
            return saveWorkspaceRoot(userId, requestedPath);
        }
        return findWorkspaceRoot(userId);
    }

    /**
     * 读取工作区根目录
     */
    private String findWorkspaceRoot(Long userId) {
        UserSetting setting = userSettingMapper.selectOne(new LambdaQueryWrapper<UserSetting>()
                .eq(UserSetting::getUserId, userId)
                .eq(UserSetting::getSettingKey, WORKSPACE_ROOT_KEY)
                .last("limit 1"));
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return null;
        }
        return setting.getSettingValue();
    }

    /**
     * 校验并保存工作区根目录
     */
    private String saveWorkspaceRoot(Long userId, String path) {
        String normalized = validateWorkspacePath(path);
        UserSetting existing = userSettingMapper.selectOne(new LambdaQueryWrapper<UserSetting>()
                .eq(UserSetting::getUserId, userId)
                .eq(UserSetting::getSettingKey, WORKSPACE_ROOT_KEY)
                .last("limit 1"));
        if (existing == null) {
            userSettingMapper.insert(UserSetting.builder()
                    .userId(userId)
                    .settingKey(WORKSPACE_ROOT_KEY)
                    .settingValue(normalized)
                    .build());
        } else {
            existing.setSettingValue(normalized);
            userSettingMapper.updateById(existing);
        }
        return normalized;
    }

    /**
     * 工作区路径校验：必须绝对路径、必须存在、必须是目录
     */
    private String validateWorkspacePath(String path) {
        if (path == null || path.isBlank()) {
            throw BusinessException.badRequest("工作区路径不能为空");
        }
        Path target;
        try {
            target = Paths.get(path.trim());
        } catch (InvalidPathException e) {
            throw BusinessException.badRequest("工作区路径不合法");
        }
        if (!target.isAbsolute()) {
            throw BusinessException.badRequest("工作区路径必须是绝对路径");
        }
        if (!Files.exists(target)) {
            throw BusinessException.badRequest("工作区路径不存在");
        }
        if (!Files.isDirectory(target)) {
            throw BusinessException.badRequest("工作区路径必须是目录");
        }
        try {
            // 存真实路径，避免符号链接导致后续校验口径不一致
            return target.toRealPath().toString();
        } catch (IOException e) {
            throw BusinessException.badRequest("工作区路径无法访问");
        }
    }
}

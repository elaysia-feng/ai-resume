package com.elias.service.auth;

import com.elias.common.UserInfoDTO;
import com.elias.dto.auth.request.LocalProfileRequest;
import com.elias.dto.auth.response.AuthResponse;

/** 本机数据会话业务契约。 */
public interface AuthService {

    /**
     * 创建或复用本地数据归属记录，并签发本机 API 请求令牌。
     *
     * @param request 本地记录的展示名称
     * @return 含本地用户编号和请求令牌的响应
     */
    AuthResponse createLocalProfile(LocalProfileRequest request);

    /** 读取当前令牌对应的本地数据归属记录。 */
    UserInfoDTO getCurrentUser();
}

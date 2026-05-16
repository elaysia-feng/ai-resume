package com.airesumeforge.client;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;


@FeignClient(name = "auth-service")
public interface UserClient {
    @GetMapping("/api/auth/me")
    ApiResponse<UserInfoDTO> getCurrentUser();
}

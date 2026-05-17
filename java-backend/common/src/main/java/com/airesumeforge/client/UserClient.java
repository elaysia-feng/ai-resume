package com.airesumeforge.client;

import com.airesumeforge.common.ApiResponse;
import com.airesumeforge.common.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "auth-service")
public interface UserClient {

    @GetMapping("/api/auth/me")
    ApiResponse<UserInfoDTO> getCurrentUser();

    @GetMapping("/api/auth/user/{userId}")
    ApiResponse<UserInfoDTO> getUserById(@PathVariable("userId") Long userId);
}
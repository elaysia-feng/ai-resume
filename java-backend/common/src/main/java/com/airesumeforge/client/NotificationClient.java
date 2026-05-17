package com.airesumeforge.client;

import com.airesumeforge.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @GetMapping("/api/notification/code/send")
    ApiResponse<Void> sendCode(
            @RequestParam String to,
            @RequestParam String fromUsername);

    @PostMapping("/api/notification/code/verify")
    ApiResponse<Boolean> verifyCode(
            @RequestParam String email,
            @RequestParam String code);
}
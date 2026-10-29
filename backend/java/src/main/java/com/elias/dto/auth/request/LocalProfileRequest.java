package com.elias.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建本地身份的请求参数（桌面端首次启动用）
 *
 * <p>这里的昵称是「账号身份」（我是谁），全局唯一：
 * 出现在应用壳侧边栏、个人档案的「账号资料」区。
 * 和简历里的求职人姓名 / 头像（PersonalInfoEditor 那一套）是两回事，
 * 简历信息存在 resume 数据里，不走这个接口。</p>
 */
@Data
public class LocalProfileRequest {

    /** 昵称：应用内展示的账号身份，去空白后长度 1-32（长度校验在 Service 里做） */
    @NotBlank(message = "昵称不能为空")
    private String displayName;
}

package com.elias.entity.notification;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮箱验证码。
 *
 * <p>原先存在 Redis 里（key = verification:{email}，5 分钟过期），
 * 现在桌面端不再依赖 Redis，改存 SQLite 的 verification_code 表。</p>
 */
@Data
@TableName("verification_code")
public class VerificationCode {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目标邮箱，同一邮箱只保留一条（发送新验证码时覆盖旧的）
     */
    private String email;

    /**
     * 验证码明文
     */
    private String code;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

package com.xie.platform.access.environment;

import com.xie.platform.model.enumValue.NetworkZone;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ABAC 环境上下文模型
 * 封装与请求本身相关的环境属性，与主体和资源无关。
 * PDP 可以利用这些属性实现时间段限制、IP 白名单等环境策略。
 */
@Data
@Builder
public class Environment {

    /**
     * 请求发起时间
     * 用于判断是否在允许的工作时间段内访问
     */
    private LocalDateTime requestTime;

    /**
     * 请求来源 IP
     * 用于判断是否来自公司内网或可信网段
     */
    private String ipAddress;
    private NetworkZone networkZone;

    /**
     * 请求路径
     * 便于后续审计时快速定位“这条决策是由哪个接口触发的”
     */
    private String requestUri;
}

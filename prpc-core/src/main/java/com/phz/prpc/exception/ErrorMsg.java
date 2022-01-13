package com.phz.prpc.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 定义若干错误信息
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月09日 14:00
 */
@Getter
@AllArgsConstructor
public enum ErrorMsg {
    /**
     * {@code nacos}服务连接失败
     **/
    CONNECT_FAILED("Nacos服务连接失败"),
    /**
     * 实例注册失败
     **/
    REGISTRY_ERROR("实例注册失败"),
    /**
     * 实例注销失败
     **/
    DE_REGISTRY_ERROR("实例注销失败"),
    /**
     * 获取实例失败
     **/
    GET_INSTANCE_ERROR("获取实例失败"),

    /**
     * 实例连接失败
     **/
    CONNECT_INSTANCE_ERROR("实例连接失败"),
    /**
     * 服务不存在
     **/
    SERVER_NOT_FOUND("服务不存在");

    /**
     * 错误信息
     **/
    private final String message;
}
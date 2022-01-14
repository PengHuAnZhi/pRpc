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
    SERVER_NOT_FOUND("服务不存在"),
    /**
     * 未知的序列化算法
     **/
    UNKNOWN_SERIALIZER_ALGORITHM("未知的序列化算法"),
    /**
     * 错误的重连次数
     **/
    ILLEGAL_RECONNECT_NUMBER("错误的重连次数"),
    /**
     * 未知的魔数
     **/
    UNKNOWN_MAGIC_CODE("未知的魔数"),
    /**
     * 反序列化失败
     **/
    DESERIALIZE_FAILED("反序列化失败"),
    /**
     * 未知的方法
     **/
    UNKNOWN_METHOD("未知的方法"),
    /**
     * 调用方法失败
     **/
    FAILED_INVOKE_METHOD("调用方法失败"),
    /**
     * 没有可用实例
     **/
    NO_MORE_INSTANCE("没有可用实例");

    /**
     * 错误信息
     **/
    private final String message;
}
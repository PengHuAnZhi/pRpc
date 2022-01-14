package com.phz.prpc.netty.message;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * {@code rpc}消息的抽象类，
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 21:35
 */
@Data
public abstract class Message implements Serializable {
    /**
     * 请求类型 {@code byte} 值
     */
    public static final int RPC_MESSAGE_TYPE_REQUEST = 1;
    /**
     * 响应类型 {@code byte} 值
     */
    public static final int RPC_MESSAGE_TYPE_RESPONSE = 2;
    /**
     * {@link PingMessage}消息类型
     */
    public static final int PING_MESSAGE = 3;
    /**
     * 消息类型对应{@link Class}类的集合
     */
    private static final Map<Integer, Class<? extends Message>> MESSAGE_CLASSES = new HashMap<>();

    /*
     * 初始化所有的消息类到集合中
     */
    static {
        MESSAGE_CLASSES.put(RPC_MESSAGE_TYPE_REQUEST, RpcRequestMessage.class);
        MESSAGE_CLASSES.put(RPC_MESSAGE_TYPE_RESPONSE, RpcResponseMessage.class);
    }

    /**
     * 根据消息类型字节，获得对应的消息{@link Class}，因为部分序列化反序列化算法需要得知原始对象的信息，所以需要维护所有消息子类的{@link Class}信息
     *
     * @param messageType 消息类型字节
     * @return 消息 {@link Class}
     */
    public static Class<? extends Message> getMessageClass(int messageType) {
        return MESSAGE_CLASSES.get(messageType);
    }

    /**
     * 请求序号
     */
    private String sequenceId;

    /**
     * 消息类型
     */
    private int messageType;

    /**
     * 获取消息的类型
     *
     * @return int 返回消息的类型
     **/
    public abstract int getMessageType();
}

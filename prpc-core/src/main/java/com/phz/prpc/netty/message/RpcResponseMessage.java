package com.phz.prpc.netty.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * <p>
 * {@code rpc}响应消息类型
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 21:37
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RpcResponseMessage extends Message {
    /**
     * 返回值
     */
    private Object returnValue;
    /**
     * 异常值
     */
    private Exception exceptionValue;

    @Override
    public int getMessageType() {
        return RPC_MESSAGE_TYPE_RESPONSE;
    }
}

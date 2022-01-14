package com.phz.prpc.netty.message;

/**
 * <p>
 * 心跳检测包
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月14日 20:24
 */
public class PingMessage extends Message {
    @Override
    public int getMessageType() {
        return PING_MESSAGE;
    }
}
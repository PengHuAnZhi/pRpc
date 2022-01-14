package com.phz.prpc.netty.handler;

import com.phz.prpc.netty.message.RpcResponseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * {@code rpc}响应消息处理器
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 21:36
 */
@Slf4j
@ChannelHandler.Sharable
public class RpcResponseMessageHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {

    /**
     * 因为服务端的方法调用结果不是立刻返回的，会有一定延迟，对于每一次发送的{@code rpc}请求消息都会生成一个唯一的消息{@code ID}，以此{@code ID}缓存所有用于接收响应的{@link Promise}对象
     **/
    public static final Map<Integer, Promise<Object>> PROMISE_MAP = new ConcurrentHashMap<>();

    /**
     * 读取{@code rpc}响应类型的消息并处理，此方法正常情况下应该是客户端方调用
     *
     * @param ctx {@link ChannelHandlerContext}处理器上下文
     * @param msg {@link RpcResponseMessage}响应消息对象
     **/
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponseMessage msg) {
        log.info("{} : 收到响应 : {}", ctx.channel().localAddress(), msg);
        Promise<Object> promise = PROMISE_MAP.get(msg.getSequenceId());
        if (promise == null) {
            return;
        }
        Exception exceptionValue = msg.getExceptionValue();
        if (exceptionValue == null) {
            promise.setSuccess(msg.getReturnValue());
        } else {
            promise.setFailure(exceptionValue);
        }
    }
}
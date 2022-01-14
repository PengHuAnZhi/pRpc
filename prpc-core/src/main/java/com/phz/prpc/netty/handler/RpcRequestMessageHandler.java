package com.phz.prpc.netty.handler;

import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.netty.message.RpcRequestMessage;
import com.phz.prpc.netty.message.RpcResponseMessage;
import com.phz.prpc.netty.server.ServiceProvider;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>
 * {@code rpc}请求消息处理器
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 21:36
 */
@ChannelHandler.Sharable
@Slf4j
public class RpcRequestMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {

    /**
     * 读取{@code rpc}请求类型的消息并处理，此方法正常情况下应该是服务端方调用
     *
     * @param ctx {@link ChannelHandlerContext}处理器上下文
     * @param msg {@link RpcRequestMessage}请求消息对象
     **/
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage msg) {
        ServiceProvider serviceProvider = ServiceProvider.getInstance();
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage();
        rpcResponseMessage.setSequenceId(msg.getSequenceId());
        String methodName = msg.getMethodName();
        String serviceName = msg.getInterfaceName() + ":" + msg.getGroupName();
        //服务提供类根据服务名选取已注册的服务class对象
        Class<?> clazz = serviceProvider.getService(serviceName);
        Method method = null;
        try {
            method = clazz.getMethod(methodName, msg.getParameterTypes());
        } catch (NoSuchMethodException e) {
            log.error("方法{}不存在", methodName);
            rpcResponseMessage.setExceptionValue(e);
            ctx.writeAndFlush(rpcResponseMessage);
            throw new PrpcException(ErrorMsg.UNKNOWN_METHOD);
        }
        Object result;
        try {
            result = method.invoke(serviceProvider.getServiceInstance(clazz), msg.getParameterValue());
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("方法{}调用失败", methodName);
            rpcResponseMessage.setExceptionValue(e);
            ctx.writeAndFlush(rpcResponseMessage);
            throw new PrpcException(ErrorMsg.FAILED_INVOKE_METHOD);
        }
        rpcResponseMessage.setReturnValue(result);
        log.info("远程方法调用成功 ： {}", result);
        ctx.writeAndFlush(rpcResponseMessage);
    }
}
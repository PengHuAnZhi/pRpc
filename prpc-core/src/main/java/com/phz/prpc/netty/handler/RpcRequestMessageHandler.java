package com.phz.prpc.netty.handler;

import com.phz.prpc.netty.message.RpcRequestMessage;
import com.phz.prpc.netty.message.RpcResponseMessage;
import com.phz.prpc.netty.server.ServiceProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

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
        try {
            String serviceName = msg.getInterfaceName() + ":" + msg.getGroupName();
            //服务提供类根据服务名选取已注册的服务class对象
            Class<?> clazz = serviceProvider.getService(serviceName);
            Method method = clazz.getMethod(msg.getMethodName(), msg.getParameterTypes());
            Object result = method.invoke(serviceProvider.getServiceInstance(clazz), msg.getParameterValue());
            rpcResponseMessage.setReturnValue(result);
        } catch (Exception e) {
            rpcResponseMessage.setExceptionValue(new Exception("远程方法调用错误 ： " + e.getMessage()));
        }
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            channel.writeAndFlush(rpcResponseMessage);
        }
    }
}
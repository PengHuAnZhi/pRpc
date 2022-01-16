package com.phz.prpc.proxy;

import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.netty.client.NettyClient;
import com.phz.prpc.netty.handler.RpcResponseMessageHandler;
import com.phz.prpc.netty.message.RpcRequestMessage;
import com.phz.prpc.spring.SpringBeanUtil;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * <p>
 * 所有动态代理类最终都是要执行同一个{@code prpc}远程方法请求，抽离出来作为公共代码
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月16日 12:07
 */
@Slf4j
public class InvokeRpcMessage {

    /**
     * {@code Prpc配置类}
     **/
    private static final PrpcProperties PRPC_PROPERTIES = SpringBeanUtil.getBean(PrpcProperties.class);

    /**
     * 客户端{@code Netty Rpc}请求实例{@link NettyClient}
     **/
    private static final NettyClient NETTY_CLIENT = NettyClient.getInstance();

    /**
     * 代理对象都需要执行这个方法，抽离出来作为公用
     *
     * @param groupName 服务组名
     * @param method    方法对象
     * @param args      方法参数
     * @return Object 代理类
     **/
    public static Object invokeRpcMessageMethod(String groupName, Method method, Object[] args) throws InterruptedException {
        String sequenceId = UUID.randomUUID().toString();
        String methodName = method.getName();
        RpcRequestMessage rpcRequestMessage = RpcRequestMessage
                .builder()
                .interfaceName(method.getDeclaringClass().getCanonicalName())
                .methodName(methodName)
                .groupName(groupName)
                .returnType(method.getReturnType())
                .parameterTypes(method.getParameterTypes())
                .parameterValue(args)
                .build();
        rpcRequestMessage.setSequenceId(sequenceId);
        boolean flag = NETTY_CLIENT.sendPrpcRequestMessage(rpcRequestMessage);
        if (!flag) {
            return null;
        }
        //创建这次Rpc请求所需要的Promise对象用于接收结果
        Promise<Object> promise = new DefaultPromise<>(new DefaultEventLoop().next());
        //将当前Promise传送到响应处理类中的一个Map
        RpcResponseMessageHandler.putPromise(sequenceId, promise);
        promise.await(PRPC_PROPERTIES.getTimeOut());
        if (promise.isSuccess()) {
            Object result = promise.getNow();
            log.info("方法{}调用成功,结果为:{}", methodName, result);
            return result;
        } else {
            log.error("方法{}调用失败,原因:{}", methodName, promise.cause());
            return null;
        }
    }
}
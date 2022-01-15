package com.phz.prpc.proxy;

import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.netty.client.NettyClient;
import com.phz.prpc.netty.handler.RpcResponseMessageHandler;
import com.phz.prpc.netty.message.RpcRequestMessage;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * 调用远程服务的代理类
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 17:15
 */
@Data
@Builder
@Slf4j
public class PrpcProxy implements InvocationHandler {

    /**
     * 服务的名称
     **/
    private String serviceName;

    /**
     * 提供服务的组名
     **/
    private String groupName;

    /**
     * 客户端{@code Netty Rpc}请求实例{@link NettyClient}
     **/
    private static final NettyClient NETTY_CLIENT = NettyClient.getInstance();

    /**
     * 真实的代理类所调用的方法
     *
     * @param proxy  要被代理的对象
     * @param method 被代理的对象的方法
     * @param args   方法中的参数值
     * @return Object  返回代理方法的返回值
     **/
    @Override
    @SneakyThrows
    public Object invoke(Object proxy, Method method, Object[] args) {
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
        //创建这次Rpc请求所需要的Promise对象用于接收结果,TODO
        Promise<Object> promise = new DefaultPromise<>(new DefaultEventLoop().next());
        //将当前Promise传送到响应处理类中的一个Map
        RpcResponseMessageHandler.putPromise(sequenceId, promise);
        promise.await();
        if (promise.isSuccess()) {
            Object result = promise.getNow();
            log.info("方法{}调用成功,结果为:{}", methodName, result);
            return result;
        } else {
            log.error("方法{}调用失败,原因:{}", methodName, promise.cause());
            throw new PrpcException(ErrorMsg.FAILED_INVOKE_METHOD);
        }
    }

    /**
     * 根据被代理对象类型创建其代理类
     *
     * @param <T>   被代理对象泛型
     * @param clazz 被代理对象的{@link Class}对象
     * @return T 返回代理对象
     **/
    public <T> T getProxy(Class<T> clazz) {
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this));
    }
}
package com.phz.prpc.proxy;

import com.phz.prpc.netty.client.NettyClient;
import com.phz.prpc.netty.message.RpcRequestMessage;
import com.phz.prpc.netty.protocol.SequenceIdGenerator;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.phz.prpc.netty.handler.RpcResponseMessageHandler.PROMISE_MAP;

/**
 * 调用远程服务的代理类
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 17:15
 */
@Data
@Builder
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
        int sequenceId = SequenceIdGenerator.nextId();
        RpcRequestMessage rpcRequestMessage = RpcRequestMessage
                .builder()
                .interfaceName(method.getDeclaringClass().getCanonicalName())
                .methodName(method.getName())
                .groupName(groupName)
                .returnType(method.getReturnType())
                .parameterTypes(method.getParameterTypes())
                .parameterValue(args)
                .build();
        rpcRequestMessage.setSequenceId(sequenceId);
        NettyClient.getInstance().sendPrpcRequestMessage(rpcRequestMessage);
        //创建这次Rpc请求所需要的Promise对象用于接收结果
        Promise<Object> promise = new DefaultPromise<>(new DefaultEventLoop().next());
        //将当前Promise传送到响应处理类中的一个Map
        PROMISE_MAP.put(sequenceId, promise);
        promise.await();
        if (promise.isSuccess()) {
            return promise.getNow();
        } else {
            throw new RuntimeException(promise.cause());
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
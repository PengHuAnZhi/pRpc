package com.phz.prpc.proxy;

import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.netty.client.NettyClient;
import com.phz.prpc.spring.SpringBeanUtil;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 调用远程服务的代理类
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 17:15
 */
@Data
@Builder
@Slf4j
public class PrpcJdkProxy implements InvocationHandler {

    /**
     * 提供服务的组名
     **/
    private String groupName;

    /**
     * 客户端{@code Netty Rpc}请求实例{@link NettyClient}
     **/
    private static final NettyClient NETTY_CLIENT = NettyClient.getInstance();

    /**
     * {@code Prpc配置类}
     **/
    private static final PrpcProperties PRPC_PROPERTIES = SpringBeanUtil.getBean(PrpcProperties.class);

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
        return InvokeRpcMessage.invokeRpcMessageMethod(groupName, method, args);
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
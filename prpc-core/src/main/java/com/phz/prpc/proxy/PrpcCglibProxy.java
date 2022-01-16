package com.phz.prpc.proxy;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * <p>
 * {@code Cglib}实现动态代理
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月16日 12:02
 */
@Data
@Builder
@Slf4j
public class PrpcCglibProxy {
    /**
     * 服务的名称
     **/
    private String serviceName;

    /**
     * 提供服务的组名
     **/
    private String groupName;


    /**
     * 根据被代理对象类型创建其代理类
     *
     * @param <T>   被代理对象泛型
     * @param clazz 被代理对象的{@link Class}对象
     * @return T 返回代理对象
     **/
    public <T> T getProxy(Class<T> clazz) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback((MethodInterceptor) (o, method, args, methodProxy) -> InvokeRpcMessage.invokeRpcMessageMethod(groupName, method, args));
        return clazz.cast(enhancer.create());
    }
}
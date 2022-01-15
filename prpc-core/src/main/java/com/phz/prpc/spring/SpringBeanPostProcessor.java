package com.phz.prpc.spring;

import com.phz.prpc.annotation.PrpcClient;
import com.phz.prpc.annotation.PrpcServer;
import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.netty.server.ServiceProvider;
import com.phz.prpc.proxy.PrpcProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * {@link BeanPostProcessor}的实现类注册到{@code IOC}容器后，对于容器所创建的每个{@code Bean}实例在初始化方法调用前，将会调用{@link BeanPostProcessor#postProcessBeforeInitialization}<br><br>
 * 而在{@code Bean}实例初始化方法调用完成后，则会调用{@link BeanPostProcessor#postProcessAfterInitialization}<br><br>
 * 整个调用顺序可以简单示意如下：<br><br>
 * --> {@code IOC}容器实例化{@code Bean}<br><br>
 * --> 调用{@link BeanPostProcessor#postProcessBeforeInitialization}<br>
 * --> 调用{@code Bean}实例的初始化方法<br>
 * --> 调用{@link BeanPostProcessor#postProcessAfterInitialization}<br><br>
 * 这里用来扫描我们所有的{@code Bean}是否有被{@link PrpcServer}以及{@link PrpcClient}所标注的{@code Bean}，然后对其做我们自己的处理<br>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 14:05
 */
@Slf4j
@Component
@DependsOn("springBeanUtil")
public class SpringBeanPostProcessor implements BeanPostProcessor {
    /**
     * 获取配置文件类对象
     **/
    @Resource
    private PrpcProperties prpcProperties;

    /**
     * {@link ServiceProvider} 所有可以提供{@code Rpc}服务的实例都需要发布到注册中心
     **/
    private final ServiceProvider serviceProvider = ServiceProvider.getInstance();

    /**
     * 实例化{@code Bean}前，校验当前{@code Bean}是否被{@link PrpcServer}注解标注，来决定是否将当前{@code Bean}注册为一个{@code Bean}服务
     *
     * @param bean     {@code Bean}对象
     * @param beanName {@code Bean}的名称
     * @return Object 返回业务逻辑处理完成后的{@code Bean}
     **/
    @Override
    public Object postProcessBeforeInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(PrpcServer.class)) {
            try {
                log.info("发现一个名为 {} 的组件，正在将其注册为Prpc服务", beanName);
                PrpcServer prpcServer = bean.getClass().getAnnotation(PrpcServer.class);
                //构造一个rpc服务名称
                String prpcServiceName = bean.getClass().getInterfaces()[0].getCanonicalName() + ":" + prpcServer.groupName();
                //获取本机Ip
                String host = InetAddress.getLocalHost().getHostAddress();
                //从配置文件中获取本地rpc服务端口号
                int port = prpcProperties.getServerPort();
                InetSocketAddress address = new InetSocketAddress(host, port);
                //向注册中心注册服务
                serviceProvider.publishService(prpcServiceName, address.getHostName(), address.getPort(), bean);
            } catch (UnknownHostException e) {
                log.error("获取本机ip失败");
            }
        }
        return bean;
    }

    /**
     * 实例化{@code Bean}后，校验当前{@code Bean}中是否有属性被{@link PrpcClient}所注解，来决定是否将当前{@code Bean}注册为一个{@code Bean}服务
     *
     * @param bean     {@code Bean}对象
     * @param beanName {@code Bean}名称
     * @return Object 返回业务逻辑处理完成后的{@code Bean}
     **/
    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        //拿到所有属性
        Field[] declaredFields = targetClass.getDeclaredFields();
        //判空
        if (declaredFields.length == 0) {
            return bean;
        }
        for (Field declaredField : declaredFields) {
            //只扫描被PrpcClient注解的属性
            if (!declaredField.isAnnotationPresent(PrpcClient.class)) {
                continue;
            }
            PrpcClient prpcClient = declaredField.getAnnotation(PrpcClient.class);
            PrpcProxy prpcProxy = PrpcProxy.builder().
                    serviceName(declaredField.getType().getCanonicalName()).
                    groupName(prpcClient.groupName()).
                    build();
            Object clientProxy = prpcProxy.getProxy(declaredField.getType());
            declaredField.setAccessible(true);
            try {
                declaredField.set(bean, clientProxy);
            } catch (IllegalAccessException e) {
                log.error("代理对象注入失败，原因为：{}", e.getMessage());
            }
        }
        return bean;
    }
}
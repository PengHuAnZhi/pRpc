package com.phz.prpc.netty.server;

import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.registry.NacosRegistry;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 服务提供者
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月11日 20:42
 */
@Slf4j
public final class ServiceProvider {

    /**
     * 以服务名为键维护所有的服务的{@link Class}对象
     **/
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    /**
     * {@link NacosRegistry} {@code Nacos}服务注册中心
     **/
    private final NacosRegistry nacosRegistry = NacosRegistry.getInstance();

    /**
     * 私有构造方法，禁用手动实例化
     **/
    private ServiceProvider() {
    }

    /**
     * {@code ServiceProviderHolder}单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link ServiceProvider#getInstance()}方法，才会加载此内部类，然后创建唯一{@link ServiceProvider}服务端
     **/
    private static class ServiceProviderHolder {
        /**
         * 单例
         **/
        private static final ServiceProvider INSTANCE = new ServiceProvider();
    }

    /**
     * 获取{@link ServiceProvider}单例对象
     *
     * @return ServiceProvider {@link ServiceProvider}单例对象
     **/
    public static ServiceProvider getInstance() {
        return ServiceProviderHolder.INSTANCE;
    }

    /**
     * 通过服务名，主机名，端口和提供服务的类发布一个服务实例
     *
     * @param serviceName 服务名
     * @param hostName    主机名
     * @param port        端口号
     * @param service     服务类
     **/
    public void publishService(String serviceName, String hostName, int port, Object service) {
        serviceMap.put(serviceName, service);
        nacosRegistry.registerService(serviceName, new InetSocketAddress(hostName, port));
    }

    /**
     * 通过服务名获取提供服务的实例对象
     *
     * @param serviceName 服务名
     * @return Object {@link com.phz.prpc.annotation.PrpcServer}所发布的服务实例
     **/
    public Object getService(String serviceName) {
        Object service = serviceMap.get(serviceName);
        if (service == null) {
            throw new PrpcException(ErrorMsg.SERVER_NOT_FOUND);
        }
        return service;
    }
}
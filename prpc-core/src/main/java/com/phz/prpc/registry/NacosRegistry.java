package com.phz.prpc.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.netty.loadBalance.PrpcLoadBalancer;
import com.phz.prpc.netty.server.NettyServer;
import com.phz.prpc.spring.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 连接{@code Nacos}服务的工具类，可以通过这个类注册，拉取，注销服务实例
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月09日 13:28
 */
@Slf4j
public final class NacosRegistry implements ServiceRegistry {
    /**
     * {@code Nacos}服务注册中心地址
     **/
    private static String nacosServerAddress;

    /**
     * {@code Nacos}命名服务
     **/
    private static NamingService namingService;

    /**
     * 负载均衡器
     **/
    private static PrpcLoadBalancer prpcLoadBalancer;

    /**
     * 将所有的服务名缓存下来
     **/
    private static final Map<String, List<InetSocketAddress>> SERVER_ADDRESS_MAP = new ConcurrentHashMap<>();

    /**
     * {@code Prpc配置类}
     **/
    private static final PrpcProperties PRPC_PROPERTIES = SpringBeanUtil.getBean(PrpcProperties.class);

    /**
     * 私有构造方法，禁用手动实例化
     **/
    private NacosRegistry() {
        try {
            nacosServerAddress = PRPC_PROPERTIES.getRegistryAddress();
            namingService = NamingFactory.createNamingService(nacosServerAddress);
            prpcLoadBalancer = PrpcLoadBalancer.getInstance();
            log.info("nacos服务连接成功:{}", nacosServerAddress);
        } catch (NacosException e) {
            e.printStackTrace();
            log.error("nacos服务 {} 连接失败, 原因 : {}", nacosServerAddress, e.getErrMsg());
            throw new PrpcException(ErrorMsg.CONNECT_FAILED);
        }
    }

    /**
     * {@code Nacos}服务单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link NacosRegistry#getInstance()}方法，才会加载此内部类，然后创建唯一注册中心实例
     **/
    private static class NacosServerHolder {
        /**
         * 单例
         **/
        private static final NacosRegistry INSTANCE = new NacosRegistry();
    }

    /**
     * 获取{@code Nacos}服务
     *
     * @return NacosServerHolder {@code Nacos}服务单例
     **/
    public static NacosRegistry getInstance() {
        return NacosServerHolder.INSTANCE;
    }

    /**
     * 注册一个远程服务
     *
     * @param serviceName 服务名称，需要唯一
     * @param address     注册服务的远程地址
     **/
    @Override
    public void registerService(String serviceName, InetSocketAddress address) {
        String hostName = address.getHostString();
        int port = address.getPort();
        try {
            namingService.registerInstance(serviceName, hostName, port);
            List<InetSocketAddress> inetSocketAddressList = SERVER_ADDRESS_MAP.computeIfAbsent(serviceName, k -> new ArrayList<>());
            inetSocketAddressList.add(address);
            NettyServer.getInstance().start();
            log.info("实例 {} {} {} 注册成功", serviceName, hostName, port);
        } catch (NacosException e) {
            log.error("实例 {} {} {} 注册失败, 原因 : {}", serviceName, hostName, port, e.getErrMsg());
        }
    }

    /**
     * 移除所有的服务实例
     **/
    @Override
    public void deRegisterAllService() {
        Iterator<Map.Entry<String, List<InetSocketAddress>>> iterator = SERVER_ADDRESS_MAP.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<InetSocketAddress>> entry = iterator.next();
            String serviceName = entry.getKey();
            List<InetSocketAddress> addresses = entry.getValue();
            addresses.forEach(address -> deRegisterService(serviceName, address.getHostString(), address.getPort()));
            iterator.remove();
        }
    }

    /**
     * 远程服务下线
     *
     * @param serviceName 服务名称
     * @param hostName    服务地址
     * @param port        服务端口
     **/
    @Override
    public void deRegisterService(String serviceName, String hostName, int port) {
        try {
            namingService.deregisterInstance(serviceName, hostName, port);
            SERVER_ADDRESS_MAP.remove(serviceName);
            log.info("实例 {} {} {} 下线", serviceName, hostName, port);
        } catch (NacosException e) {
            log.error("实例 {} {} {} 下线失败, 原因 : {}", serviceName, hostName, port, e.getErrMsg());
            throw new PrpcException(ErrorMsg.DE_REGISTRY_ERROR);
        }
    }


    /**
     * 使用负载均衡算法获取可提供的服务实例{@link InetSocketAddress}
     *
     * @param serviceName 需要获取的服务名称
     * @return InetSocketAddress 可提供服务的实例
     **/
    @Override
    public InetSocketAddress getOneServiceInstance(String serviceName) {
        List<InetSocketAddress> serviceInstances = getServiceInstances(serviceName);
        if (CollectionUtils.isEmpty(serviceInstances)) {
            return null;
        }
        return prpcLoadBalancer.doChoice(serviceInstances);
    }

    /**
     * 根据服务名称查询下方所有的实例
     *
     * @param serviceName 服务名称
     * @return List<InetSocketAddress>  实例集合
     **/
    @Override
    public List<InetSocketAddress> getServiceInstances(String serviceName) {
        try {
            List<InetSocketAddress> instances = new ArrayList<>();
            namingService.getAllInstances(serviceName).forEach(instance -> instances.add(new InetSocketAddress(instance.getIp(), instance.getPort())));
            return instances;
        } catch (NacosException e) {
            log.error("查询服务名为 {} 的实例出现错误，错误详情 : {}", serviceName, e.getErrMsg());
            throw new PrpcException(ErrorMsg.GET_INSTANCE_ERROR);
        }
    }
}
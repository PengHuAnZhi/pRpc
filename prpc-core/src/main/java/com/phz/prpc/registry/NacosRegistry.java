package com.phz.prpc.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.spring.SpringBeanUtil;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.netty.server.NettyServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 连接{@code Nacos}服务的工具类，可以通过这个类注册，拉取，注销服务实例
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月09日 13:28
 */
@Slf4j
public class NacosRegistry {
    /**
     * {@code Nacos}服务注册中心地址
     **/
    private static String NACOS_SERVER_ADDRESS;

    /**
     * {@code Nacos}命名服务
     **/
    private static NamingService NAMING_SERVICE;

    /**
     * 将所有的服务名缓存下来
     **/
    private static final Map<String, InetSocketAddress> SERVER_ADDRESS_MAP = new HashMap<>();

    /**
     * 私有构造方法，禁用手动实例化
     **/
    private NacosRegistry() {
        try {
            NACOS_SERVER_ADDRESS = SpringBeanUtil.getBean(PrpcProperties.class).getNacosAddress();
            NAMING_SERVICE = NamingFactory.createNamingService(NACOS_SERVER_ADDRESS);
            log.info("nacos服务连接成功:{}", NACOS_SERVER_ADDRESS);
        } catch (NacosException e) {
            e.printStackTrace();
            log.error("nacos服务 {} 连接失败, 原因 : {}", NACOS_SERVER_ADDRESS, e.getErrMsg());
            throw new PrpcException(ErrorMsg.CONNECT_FAILED);
        }
    }

    /**
     * {@code Nacos}服务单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link NacosRegistry#getInstance()}方法，才会加载此内部类，然后创建唯一注册中心实例
     **/
    private static class NacosServerHolder {
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
    public void registerService(String serviceName, InetSocketAddress address) {
        String hostName = address.getHostString();
        int port = address.getPort();
        try {
            NAMING_SERVICE.registerInstance(serviceName, hostName, port);
            SERVER_ADDRESS_MAP.put(serviceName, address);
            NettyServer.getInstance().start();
            log.info("实例 {} {} {} 注册成功", serviceName, hostName, port);
        } catch (NacosException e) {
            log.error("实例 {} {} {} 注册失败, 原因 : {}", serviceName, hostName, port, e.getErrMsg());
        }
    }

    /**
     * 远程服务下线
     *
     * @param serviceName 服务名称
     * @param address     服务地址
     **/
    public void deRegisterService(String serviceName, InetSocketAddress address) {
        deRegisterService(serviceName, address.getHostString(), address.getPort());
    }

    /**
     * 移除所有的服务实例
     **/
    public void deRegisterAllService() {
        Iterator<Map.Entry<String, InetSocketAddress>> iterator = SERVER_ADDRESS_MAP.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, InetSocketAddress> entry = iterator.next();
            String serviceName = entry.getKey();
            InetSocketAddress address = entry.getValue();
            deRegisterService(serviceName, address.getHostString(), address.getPort());
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
    public void deRegisterService(String serviceName, String hostName, int port) {
        try {
            NAMING_SERVICE.deregisterInstance(serviceName, hostName, port);
            SERVER_ADDRESS_MAP.remove(serviceName);
            log.info("实例 {} {} {} 下线", serviceName, hostName, port);
        } catch (NacosException e) {
            log.error("实例 {} {} {} 下线失败, 原因 : {}", serviceName, hostName, port, e.getErrMsg());
            throw new PrpcException(ErrorMsg.DE_REGISTRY_ERROR);
        }
    }

    /**
     * 根据服务名称查询下方所有的实例
     *
     * @param serviceName 服务名称
     * @return List<Instance>  实例集合
     **/
    public List<Instance> getInstances(String serviceName) {
        try {
            return NAMING_SERVICE.getAllInstances(serviceName);
        } catch (NacosException e) {
            log.error("查询服务名为 {} 的实例出现错误，错误详情 : {}", serviceName, e.getErrMsg());
            throw new PrpcException(ErrorMsg.GET_INSTANCE_ERROR);
        }
    }
}
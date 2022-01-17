package com.phz.prpc.registry;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author PengHuanZhi
 * @date 2022年01月16日 13:52
 */
public interface ServiceRegistry {

    /**
     * 注册一个远程服务
     *
     * @param serviceName 服务名称，需要唯一
     * @param address     注册服务的远程地址
     **/
    void registerService(String serviceName, InetSocketAddress address);

    /**
     * 移除所有的服务实例
     **/
    void deRegisterAllService();

    /**
     * 远程服务下线
     *
     * @param serviceName 服务名称
     * @param hostName    服务地址
     * @param port        服务端口
     **/
    void deRegisterService(String serviceName, String hostName, int port);

    /**
     * 使用负载均衡算法获取可提供的服务实例{@link InetSocketAddress}
     *
     * @param serviceName 需要获取的服务名称
     * @return InetSocketAddress 可提供服务的实例
     **/
    InetSocketAddress getOneServiceInstance(String serviceName);

    /**
     * 根据服务名称查询下方所有的实例
     *
     * @param serviceName 服务名称
     * @return List<InetSocketAddress>  实例集合
     **/
    List<InetSocketAddress> getServiceInstances(String serviceName);
}
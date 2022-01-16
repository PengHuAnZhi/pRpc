package com.phz.prpc.registry;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * <p>
 * {@code Consul}做注册中心的实现
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月16日 16:40
 */
public class ConsulRegistry implements ServiceRegistry {
    @Override
    public void registerService(String serviceName, InetSocketAddress address) {

    }

    @Override
    public void deRegisterAllService() {

    }

    @Override
    public void deRegisterService(String serviceName, String hostName, int port) {

    }

    @Override
    public InetSocketAddress getOneServiceInstance(String serviceName) {
        return null;
    }

    @Override
    public List<InetSocketAddress> getServiceInstances(String serviceName) {
        return null;
    }
}

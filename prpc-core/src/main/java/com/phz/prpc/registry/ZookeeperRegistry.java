package com.phz.prpc.registry;

import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.netty.loadBalance.PrpcLoadBalancer;
import com.phz.prpc.netty.server.NettyServer;
import com.phz.prpc.spring.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * {@code Zookeeper}做服务注册中心的实现
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月16日 12:40
 */
@Slf4j
public final class ZookeeperRegistry implements ServiceRegistry {

    /**
     * {@code Zookeeper}客户端
     **/
    private static CuratorFramework zkClient;

    /**
     * {@code prpc} 服务在{@code Zookeeper}中保存的根目录
     **/
    private static String rootPath;

    /**
     * 维护一个已经注册服务集合
     **/
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();


    /**
     * 将所有注册的实例缓存下来
     **/
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();

    /**
     * {@code Prpc配置类}
     **/
    private static final PrpcProperties PRPC_PROPERTIES = SpringBeanUtil.getBean(PrpcProperties.class);

    /**
     * 负载均衡器
     **/
    private static PrpcLoadBalancer prpcLoadBalancer;

    /**
     * 私有构造方法，禁用手动实例化
     **/
    private ZookeeperRegistry() {
        rootPath = "/" + PRPC_PROPERTIES.getZookeeperRootPath();
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(PRPC_PROPERTIES.getZookeeperRetryBaseTime(), PRPC_PROPERTIES.getZookeeperRetryTimes());
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(PRPC_PROPERTIES.getRegistryAddress())
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        prpcLoadBalancer = PrpcLoadBalancer.getInstance();
        try {
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * {@link org.apache.zookeeper.ZooKeeper}服务单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link ZookeeperRegistry#getInstance()}方法，才会加载此内部类，然后创建唯一注册中心实例
     **/
    private static class ZookeeperRegistryHolder {
        /**
         * 单例
         **/
        private static final ZookeeperRegistry INSTANCE = new ZookeeperRegistry();
    }

    /**
     * 获取{@link org.apache.zookeeper.ZooKeeper}
     *
     * @return ZookeeperRegistry {@link org.apache.zookeeper.ZooKeeper}服务单例
     **/
    public static ZookeeperRegistry getInstance() {
        return ZookeeperRegistryHolder.INSTANCE;
    }

    /**
     * {@link ServiceRegistry#registerService}
     **/
    @Override
    public void registerService(String serviceName, InetSocketAddress address) {
        String path = rootPath + "/" + serviceName + "/" + address.getAddress().getHostAddress() + ":" + address.getPort();
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("节点 {} 已注册", path);
            } else {
                /*
                 * 比如/prpc/com.phz.prpc.server.HelloService/127.0.0.1:9999
                 **/
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("节点 {} 创建成功", path);
            }
            REGISTERED_PATH_SET.add(path);
            NettyServer.getInstance().start();
        } catch (Exception e) {
            log.error("创建节点 {} 失败，原因 : {}", path, e.getMessage());
        }
    }

    /**
     * {@link ServiceRegistry#deRegisterAllService}
     **/
    @Override
    public void deRegisterAllService() {
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                zkClient.delete().forPath(p);
            } catch (Exception e) {
                log.error("节点 {} 取消注册失败", p);
            }
        });
    }

    /**
     * {@link ServiceRegistry#deRegisterService}
     **/
    @Override
    public void deRegisterService(String serviceName, String hostName, int port) {
        String path = serviceName + "/" + hostName + ":" + port;
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(path)) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("节点 {} 取消注册失败", p);
            }
        });
        log.info("所有节点都已注销完毕: {}", REGISTERED_PATH_SET);
    }

    /**
     * {@link ServiceRegistry#getOneServiceInstance}
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
     * {@link ServiceRegistry#getServiceInstances}
     **/
    @Override
    public List<InetSocketAddress> getServiceInstances(String serviceName) {
        if (SERVICE_ADDRESS_MAP.containsKey(serviceName)) {
            List<String> addressStrings = SERVICE_ADDRESS_MAP.get(serviceName);
            List<InetSocketAddress> instances = new ArrayList<>();
            for (String addressString : addressStrings) {
                String[] hostAndPort = addressString.split(":");
                instances.add(new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])));
            }
            return instances;
        }
        List<InetSocketAddress> instances = new ArrayList<>();
        String servicePath = rootPath + "/" + serviceName;
        try {
            List<String> addressStrings = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(serviceName, addressStrings);
            for (String addressString : addressStrings) {
                String[] hostAndPort = addressString.split(":");
                instances.add(new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])));
            }
        } catch (Exception e) {
            log.error("获取服务 {} 失败,原因为 : {}", servicePath, e.getMessage());
        }
        return instances;
    }
}
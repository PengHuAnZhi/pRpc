package com.phz.prpc.netty.loadBalance;

import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.extension.ExtensionLoader;
import com.phz.prpc.spring.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author PengHuanZhi
 * @date 2022年01月15日 13:13
 */
@Slf4j
public final class PrpcLoadBalancer {
    /**
     * {@code Prpc}配置类
     **/
    private static PrpcProperties prpcProperties;

    /**
     * 私有构造方法，禁用手动实例化
     **/
    private PrpcLoadBalancer() {
        prpcProperties = SpringBeanUtil.getBean(PrpcProperties.class);
    }


    /**
     * {@code PrpcLoadBalance}单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link PrpcLoadBalancer#getInstance()}方法，才会加载此内部类，然后创建唯一负载均衡器
     **/
    private static class PrpcLoadBalanceHolder {
        /**
         * 单例
         **/
        private static final PrpcLoadBalancer INSTANCE = new PrpcLoadBalancer();
    }

    /**
     * 获取负载均衡器单例
     *
     * @return PrpcLoadBalance {@link PrpcLoadBalancer}负载均衡器单例
     **/
    public static PrpcLoadBalancer getInstance() {
        return PrpcLoadBalanceHolder.INSTANCE;
    }

    /**
     * 使用负载均衡算法从服务集合中选取一个服务
     *
     * @param serviceInstances 服务集合
     * @return InetSocketAddress 选取的服务
     **/
    public InetSocketAddress doChoice(List<InetSocketAddress> serviceInstances) {
        String loadBalanceAlgorithm = prpcProperties.getLoadBalanceAlgorithm();
        LoadBalance loadBalance;
        try {
            loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension();
            if (loadBalance == null) {
                loadBalance = LoadBalanceAlgorithm.valueOf(loadBalanceAlgorithm);
            }
        } catch (IllegalArgumentException e) {
            log.error("未知的负载均衡算法:{},异常信息为:{}", loadBalanceAlgorithm, e.getMessage());
            throw new PrpcException(ErrorMsg.UNKNOWN_LOAD_BALANCE_ALGORITHM);
        }
        return loadBalance.doChoice(serviceInstances);
    }
}
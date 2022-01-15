package com.phz.prpc.netty.loadBalance;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.spring.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author PengHuanZhi
 * @date 2022年01月15日 13:13
 */
@Slf4j
public class PrpcLoadBalancer {
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
     * @return Instance 选取的服务
     **/
    public Instance doChoice(List<Instance> serviceInstances) {
        String loadBalanceAlgorithm = prpcProperties.getLoadBalanceAlgorithm();
        LoadBalanceAlgorithm algorithm;
        try {
            algorithm = LoadBalanceAlgorithm.valueOf(loadBalanceAlgorithm.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("未知的负载均衡算法:{},异常信息为:{}", loadBalanceAlgorithm, e.getMessage());
            throw new PrpcException(ErrorMsg.UNKNOWN_LOAD_BALANCE_ALGORITHM);
        }
        return (Instance) algorithm.doChoice(serviceInstances);
    }
}
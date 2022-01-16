package com.phz.prpc.netty.loadBalance;

import com.phz.prpc.extension.Spi;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * <p>
 * 负载均衡算法抽象接口，所有负载均衡算法都需要实现此接口
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月15日 13:28
 */
@Spi
public interface LoadBalance {
    /**
     * 传入一个目标实例集合，通过实现的负载均衡算法，得到其中一个实例
     *
     * @param instances 实例集合
     * @return Object 返回其中一个实例
     **/
    default InetSocketAddress doChoice(List<InetSocketAddress> instances) {
        throw new UnsupportedOperationException("不支持此操作！");
    }

    /**
     * 传入一个目标实例集合，并传入一个附加参数，部分负载均衡算法可能会需要，但不是每个都需要，所以预留此方法
     *
     * @param instances      实例集合
     * @param extraParameter 附加参数
     * @return Object 返回其中一个实例
     **/
    default InetSocketAddress doChoice(List<InetSocketAddress> instances, String extraParameter) {
        if (extraParameter == null) {
            return doChoice(instances);
        } else {
            throw new UnsupportedOperationException("不支持此操作！");
        }
    }
}
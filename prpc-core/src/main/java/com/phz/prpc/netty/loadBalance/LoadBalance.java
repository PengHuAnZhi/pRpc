package com.phz.prpc.netty.loadBalance;

import java.util.List;

/**
 * <p>
 * 负载均衡算法抽象接口，所有负载均衡算法都需要实现此接口
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月15日 13:28
 */
public interface LoadBalance {
    /**
     * 传入一个目标实例集合，通过实现的负载均衡算法，得到其中一个实例
     *
     * @param instances 实例集合
     * @return Object 返回其中一个实例
     **/
    <T> Object doChoice(List<T> instances);
}
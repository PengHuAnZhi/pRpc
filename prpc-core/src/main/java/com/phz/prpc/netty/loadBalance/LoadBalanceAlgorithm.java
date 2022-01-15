package com.phz.prpc.netty.loadBalance;

import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;

/**
 * @author PengHuanZhi
 * @date 2022年01月15日 13:26
 */
public enum LoadBalanceAlgorithm implements LoadBalance {
    /**
     * 随机法
     **/
    RANDOM {
        @Override
        public <T> Object doChoice(List<T> instances) {
            if (CollectionUtils.isEmpty(instances)) {
                return null;
            } else if (instances.size() == 1) {
                return instances.get(0);
            }
            return instances.get(new Random().nextInt(instances.size()));
        }
    }
}
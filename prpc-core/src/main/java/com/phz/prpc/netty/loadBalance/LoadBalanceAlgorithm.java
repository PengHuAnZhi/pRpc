package com.phz.prpc.netty.loadBalance;

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
            return instances.get(new Random().nextInt(instances.size()));
        }
    },
    POLLING {
        /**
         * 轮询下标
         **/
        private int index = 0;

        @Override
        public <T> Object doChoice(List<T> instances) {
            return instances.get(index++ % instances.size());
        }
    }
}
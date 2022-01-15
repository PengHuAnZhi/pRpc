package com.phz.prpc.netty.loadBalance;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

/**
 * @author PengHuanZhi
 * @date 2022年01月15日 13:26
 */
@Slf4j
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
    },
    HASH {
        @Override
        public <T> Object doChoice(List<T> instances) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                int ipHash = localHost.hashCode();
                return instances.get(ipHash + 1 % instances.size());
            } catch (UnknownHostException e) {
                log.error("源地址hash失败，原因:{}", e.getMessage());
                return null;
            }
        }
    }
}
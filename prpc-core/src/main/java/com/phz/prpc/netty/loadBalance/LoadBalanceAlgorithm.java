package com.phz.prpc.netty.loadBalance;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author PengHuanZhi
 * @date 2022年01月15日 13:26
 */
@Slf4j
public enum LoadBalanceAlgorithm implements LoadBalance {
    /**
     * 随机法
     **/
    random {
        @Override
        public InetSocketAddress doChoice(List<InetSocketAddress> instances) {
            return instances.get(new Random().nextInt(instances.size()));
        }
    },
    polling {
        /**
         * 轮询下标
         **/
        private int index = 0;

        @Override
        public InetSocketAddress doChoice(List<InetSocketAddress> instances) {
            return instances.get(index++ % instances.size());
        }
    },
    hash {
        @Override
        public InetSocketAddress doChoice(List<InetSocketAddress> instances) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                int ipHash = FNV1_32_HASH.getHash(localHost.getHostAddress());
                return instances.get((ipHash + 1) % instances.size());
            } catch (UnknownHostException e) {
                log.error("源地址hash失败，原因:{}", e.getMessage());
                return null;
            }
        }
    },
    consistentHash {

        /**
         * 一致性哈希算法选择器
         **/
        private final ConcurrentHashMap<String, ConsistenceHashChooser> consistenceHashMap = new ConcurrentHashMap<>();

        @Override
        public InetSocketAddress doChoice(List<InetSocketAddress> instances, String rpcServiceName) {
            ConsistenceHashChooser consistenceHashChooser = consistenceHashMap.get(rpcServiceName);
            if (consistenceHashChooser == null) {
                consistenceHashChooser = new ConsistenceHashChooser(100, instances);
                consistenceHashMap.put(rpcServiceName, consistenceHashChooser);
            }
            InetAddress localHost;
            try {
                localHost = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                log.error("获取本机Ip失败，原因:{}", e.getMessage());
                return null;
            }
            return consistenceHashChooser.getServer(localHost.getHostAddress());
        }
    }
}
package com.phz.loadbalance;

import com.phz.prpc.netty.loadBalance.LoadBalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * @author PengHuanZhi
 * @date 2022年01月16日 19:13
 */
public class RandomLoadBalancer implements LoadBalance {
    @Override
    public InetSocketAddress doChoice(List<InetSocketAddress> instances) {
        return instances.get(new Random().nextInt(instances.size()));
    }
}
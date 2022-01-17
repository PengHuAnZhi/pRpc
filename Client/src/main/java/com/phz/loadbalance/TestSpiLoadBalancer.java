package com.phz.loadbalance;

import com.phz.prpc.netty.loadBalance.LoadBalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author PengHuanZhi
 * @date 2022年01月16日 19:13
 */
public class TestSpiLoadBalancer implements LoadBalance {
    @Override
    public InetSocketAddress doChoice(List<InetSocketAddress> instances) {
        return instances.get(1);
    }
}
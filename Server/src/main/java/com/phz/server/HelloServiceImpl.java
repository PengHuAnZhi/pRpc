package com.phz.server;

import com.phz.prpc.annotation.PrpcServer;

/**
 * @author PengHuanZhi
 * @date 2022年01月11日 21:25
 */
@PrpcServer(groupName = "hello")
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String value) {
        return "你好" + value;
    }
}
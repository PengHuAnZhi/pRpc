package com.phz.server;

import com.phz.prpc.annotation.PrpcServer;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * @author PengHuanZhi
 * @date 2022年01月11日 21:25
 */
@PrpcServer(groupName = "hello")
public class HelloServiceImpl implements HelloService {

    @Resource
    private Environment environment;

    @Override
    public String hello(String value) {
        return "你好:" + value + "(来自" + environment.getProperty("local.server.port") + ")";
    }
}
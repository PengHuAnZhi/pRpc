package com.phz.server;

import com.phz.prpc.annotation.PrpcServer;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * @author PengHuanZhi
 * @date 2022年01月16日 17:14
 */
@PrpcServer(groupName = "hello1")
public class HiServiceImpl implements HelloService {

    @Resource
    private Environment environment;

    @Override
    public String hello(String value) {
        return "Hi:" + value + "(来自" + environment.getProperty("local.server.port") + ")";
    }
}

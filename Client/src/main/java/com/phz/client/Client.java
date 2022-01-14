package com.phz.client;

import com.phz.prpc.annotation.PrpcClient;
import com.phz.server.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author PengHuanZhi
 * @date 2022年01月11日 21:23
 */
@RestController
public class Client {
    /**
     * 被代理的服务
     **/
    @PrpcClient(groupName = "hello")
    private HelloService service;

    @GetMapping("/hello")
    public String hello() {
        return service.hello("彭焕智");
    }
}
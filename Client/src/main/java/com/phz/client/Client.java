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
    @PrpcClient(groupName = "hello1")
    private HelloService service1;
    @PrpcClient(groupName = "hello2")
    private HelloService service2;

    @GetMapping("/hello")
    public String hello() {
        return service1.hello("彭焕智  ") + service2.hello("焕智");
    }
}
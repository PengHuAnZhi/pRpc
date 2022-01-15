package com.phz.prpc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 从{@code classpath}下方加载我们的{@code application.yml}配置文件，并只载入{@code prpc}节点下方的所有配置
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月09日 16:57
 */
@Data
@Component
@ConfigurationProperties(prefix = "prpc")
@PropertySource("classpath:application.yml")
public class PrpcProperties {
    /**
     * {@code nacos}的地址
     **/
    private String nacosAddress = "localhost:8848";
    /**
     * {@code rpc}服务端口地址
     **/
    private Integer serverPort = 9908;
    /**
     * 序列化算法
     **/
    private String serializerAlgorithm = "JSON";

    /**
     * 超时重连的情况
     **/
    private Integer reConnectNumber = 100;
}
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
     * {@code Nacos}的地址
     **/
    private String nacosAddress = "localhost:8848";
    /**
     * {@code Rpc}服务端口地址
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

    /**
     * {@code Rpc}请求超时事件
     **/
    private Integer timeOut = 2000;

    /**
     * {@code Rpc}从注册中心拉取到可用服务后，使用何种负载均衡算法选择服务
     **/
    private String loadBalanceAlgorithm = "random";

    /**
     * 一致性{@code Hash} {@link com.phz.prpc.netty.loadBalance.LoadBalanceAlgorithm#consistentHash}需要配置此参数，表示虚拟节点数
     **/
    private Integer virtualNodeNum = 100;

    /**
     * 压缩算法
     **/
    private String compressAlgorithm = "GZIP";

    /**
     * 动态代理实现方式
     **/
    private String proxy = "CGLIB";

    /**
     * 注册中心
     **/
    private String registry;

    /**
     * {@code Zookeeper}注册中心地址
     **/
    private String zookeeperAddress = "localhost:2181";

    /**
     * {@code Zookeeper} {@code prpc} 服务根目录
     **/
    private String zookeeperRootPath = "prpc";

    /**
     * {@code Zookeeper}重连间隔时间单位{@code ms}
     **/
    private Integer zookeeperRetryBaseTime = 1000;

    /**
     * {@code Zookeeper}重连次数
     **/
    private Integer zookeeperRetryTimes = 3;
}
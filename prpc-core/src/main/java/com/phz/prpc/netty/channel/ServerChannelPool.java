package com.phz.prpc.netty.channel;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 客户端在发送请求的时候，首先会去服务注册中心去拉取可用服务，然后通过返回的服务主机名端口号尝试连接服务，连接完成后，将存入当前{@link ServerChannelPool}，以便维护与之连接的{@link Channel}
 * </p><br></br>
 * <p>应当注意，本类正常情况下只会存在于客户端，当客户端下线后，应该将当前维护好的所有{@link Channel}注销关闭</p>
 *
 * @author PengHuanZhi
 * @date 2022年01月11日 17:24
 */
public final class ServerChannelPool {

    /**
     * 以{@code hostName:port}为键缓存当前客户端所连接的所有服务对应的通信{@link Channel}
     **/
    private static final Map<String, Channel> CHANNEL_POOL = new HashMap<>();

    /**
     * 私有构造方法，禁用手动实例化
     **/
    private ServerChannelPool() {
    }

    /**
     * {@link ServerChannelPool}单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link ServerChannelPool#getInstance()}方法，才会加载此内部类，然后创建唯一{@link ServerChannelPool}
     **/
    private static class ServerChannelPoolHolder {
        /**
         * 单例对象
         **/
        private static final ServerChannelPool INSTANCE = new ServerChannelPool();
    }

    /**
     * 公共的获取{@link ServerChannelPool}单例的方法
     *
     * @return ServerChannel {@link ServerChannelPool}池唯一单例
     **/
    public static ServerChannelPool getInstance() {
        return ServerChannelPoolHolder.INSTANCE;
    }

    /**
     * 往{@link ServerChannelPool}中添加{@link Channel}
     *
     * @param hostName 主机名
     * @param port     端口
     * @param channel  {@link Channel}对象
     **/
    public void putChannel(String hostName, int port, Channel channel) {
        CHANNEL_POOL.put(hostName + ":" + port, channel);
    }

    /**
     * 移除一个{@link Channel}
     *
     * @param hostName 主机名
     * @param port     端口
     **/
    public void removeChannel(String hostName, int port) {
        CHANNEL_POOL.remove(hostName + ":" + port);
    }

    /**
     * 根据服务主机名端口获取对应的{@link Channel}
     *
     * @param hostName 主机名
     * @param port     端口
     * @return Channel {@link Channel}对象
     **/
    public Channel getChannel(String hostName, int port) {
        String key = hostName + ":" + port;
        Channel channel = CHANNEL_POOL.get(key);
        if (channel != null && channel.isActive()) {
            return channel;
        }
        CHANNEL_POOL.remove(key);
        return null;
    }
}
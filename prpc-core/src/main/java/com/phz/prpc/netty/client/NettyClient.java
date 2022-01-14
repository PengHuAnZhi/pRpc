package com.phz.prpc.netty.client;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.netty.channel.ServerChannelPool;
import com.phz.prpc.netty.handler.RpcResponseMessageHandler;
import com.phz.prpc.netty.message.PingMessage;
import com.phz.prpc.netty.message.RpcRequestMessage;
import com.phz.prpc.netty.protocol.MessageCodecSharable;
import com.phz.prpc.netty.protocol.ProtocolFrameDecoder;
import com.phz.prpc.registry.NacosRegistry;
import com.phz.prpc.spring.SpringBeanUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.nio.channels.Selector;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 一个{@code prpc}客户端所拥有的唯一发送{@code Netty}网络请求服务的实例，通过注册中心获取到的目标可用服务集合，通过一定的策略从中选取一个服务并请求
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月11日 9:38
 */
@Slf4j
public final class NettyClient {
    /**
     * {@code Netty}请求的启动类对象
     **/
    private final Bootstrap bootstrap;
    /**
     * 与服务端连接的{@link ServerChannelPool }
     **/
    private final ServerChannelPool serverChannelPool;
    /**
     * {@code Netty}请求事件循环组，默认循环对象数为当前系统核心数*2，其中一个事件循环对象可以理解为一个单线程的线程池+{@link Selector}
     **/
    private final NioEventLoopGroup group;

    /**
     * 默认重连次数为5
     **/
    private static final int DEFAULT_RECONNECT_NUMBER = 5;

    /**
     * 私有构造方法，禁用手动实例化<br>
     * 第一次加载会将{@link ServerChannelPool }单例取出赋值到当前类属性，然后创建一个{@link NioEventLoopGroup}，最后使用这个请求事件循环组创建好一个{@code Netty}网络请求对象
     **/
    private NettyClient() {
        serverChannelPool = ServerChannelPool.getInstance();
        group = new NioEventLoopGroup();
        LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
        MessageCodecSharable messageCodecSharable = new MessageCodecSharable();
        RpcResponseMessageHandler rpcResponseMessageHandler = new RpcResponseMessageHandler();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                //是否开启 TCP 底层心跳机制
                .option(ChannelOption.SO_KEEPALIVE, true)
                //TCP默认开启了 Nagle 算法，该算法的作用是尽可能发送大数据快，减少网络传输。
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOW_HALF_CLOSURE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 用来判断是不是 读空闲时间过长，或写空闲时间过长
                        // 3s 内如果没有向服务器写数据，会触发一个 IdleState#WRITER_IDLE 事件
                        ch.pipeline().addLast(new IdleStateHandler(0, 3, 0));
                        // ChannelDuplexHandler 可以同时作为入站和出站处理器
                        ch.pipeline().addLast(new ChannelDuplexHandler() {
                            /**
                             * 用来触发特殊事件
                             **/
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                if (evt instanceof IdleStateEvent) {
                                    IdleStateEvent event = (IdleStateEvent) evt;
                                    // 触发了写空闲事件
                                    if (event.state() == IdleState.WRITER_IDLE) {
                                        log.info("3s 没有写数据了，发送一个心跳包");
                                        ctx.writeAndFlush(new PingMessage());
                                    }
                                } else if (evt instanceof ChannelInputShutdownEvent) {
                                    log.info("一个连接的远端关闭");
                                }
                            }

                            /**
                             * 用来触发异常事件
                             **/
                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                log.info("发现异常 : {}", cause.getMessage());
                                Channel channel = ctx.channel();
                                Channel parent = channel.parent();
                                if (parent != null) {
                                    parent.close();
                                } else {
                                    channel.close();
                                }
                            }
                        });
                        ch.pipeline().addLast(new ProtocolFrameDecoder());
                        ch.pipeline().addLast(loggingHandler);
                        ch.pipeline().addLast(messageCodecSharable);
                        ch.pipeline().addLast(rpcResponseMessageHandler);
                    }
                });
    }

    /**
     * {@code Netty}请求客户端单例维护静态内部类
     **/
    private static class NettyClientHolder {
        /**
         * 单例对象
         **/
        private static final NettyClient INSTANCE = new NettyClient();
    }

    /**
     * 公共的获取{@code Netty}请求客户端单例方法
     *
     * @return NettyClient 单例对象
     **/
    public static NettyClient getInstance() {
        return NettyClientHolder.INSTANCE;
    }


    /**
     * 通过主机名和端口号获取{@link Channel}，如果没有找到，会尝试重连
     *
     * @param hostName 主机名
     * @param port     端口号
     * @return Channel 目标服务的{@link Channel}
     **/
    public Channel getPrpcChannel(String hostName, int port) {
        Channel channel = serverChannelPool.getChannel(hostName, port);
        if (channel == null) {
            channel = doConnect(hostName, port);
            serverChannelPool.putChannel(hostName, port, channel);
        }
        return channel;
    }

    /**
     * 通过主机名端口连接远程服务，连接的时候可能会连接失败，如果连接失败会尝试重连，重连次数可配置，默认为5次
     *
     * @param hostName 主机名
     * @param port     端口号
     * @return Channel 目标服务的{@link Channel}
     **/
    private Channel doConnect(String hostName, int port) {
        PrpcProperties prpcProperties = SpringBeanUtil.getBean(PrpcProperties.class);
        Integer reConnectNumber = prpcProperties.getReConnectNumber();
        if (reConnectNumber == null) {
            return doConnect(hostName, port, DEFAULT_RECONNECT_NUMBER);
        }
        if (reConnectNumber <= 0) {
            log.error("错误的重连次数:{}", reConnectNumber);
            throw new PrpcException(ErrorMsg.ILLEGAL_RECONNECT_NUMBER);
        }
        return doConnect(hostName, port, reConnectNumber);
    }

    /**
     * 携带重连次数的连接方法 {@link NettyClient#doConnect}
     *
     * @param hostName        主机名
     * @param port            端口号
     * @param reConnectNumber 重连次数
     * @return Channel 目标服务的{@link Channel}
     **/
    @SneakyThrows
    private Channel doConnect(String hostName, int port, int reConnectNumber) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        ChannelFuture channelFuture = bootstrap.connect(hostName, port);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                completableFuture.complete(future.channel());
            } else if (reConnectNumber <= 0) {
                future.channel().close();
                completableFuture.completeExceptionally(new PrpcException(ErrorMsg.CONNECT_INSTANCE_ERROR));
                log.error("{}:{} 连接失败！", hostName, port);
                serverChannelPool.removeChannel(hostName, port);
                throw new PrpcException(ErrorMsg.CONNECT_INSTANCE_ERROR);
            } else {
                log.error("{} {} 连接异常，正在重连...", hostName, port);
                bootstrap.config().group().schedule(() -> {
                            doConnect(hostName, port, reConnectNumber - 1);
                        },
                        0,
                        TimeUnit.SECONDS);
            }
        });
        channelFuture.channel().closeFuture().addListener((ChannelFutureListener) future -> close());
        return completableFuture.get();
    }

    /**
     * 代理类{@link com.phz.prpc.proxy.PrpcProxy#invoke}发送消息会调用这个方法
     *
     * @param requestMessage 要发送的消息对象
     * @return boolean 返回消息是否发送成功
     **/
    public boolean sendPrpcRequestMessage(RpcRequestMessage requestMessage) {
        List<Instance> instances = NacosRegistry.getInstance().getInstances(requestMessage.getInterfaceName() + ":" + requestMessage.getGroupName());
        if (CollectionUtils.isEmpty(instances)) {
            log.error("没有可用实例");
            return false;
        }
        //TODO 负载均衡
        Instance instance = instances.get(0);
        String ip = instance.getIp();
        int port = instance.getPort();
        Channel prpcChannel = getPrpcChannel(ip, port);
        log.info("客户端向 {}:{} 发送消息:{}", ip, port, requestMessage);
        prpcChannel.writeAndFlush(requestMessage);
        return true;
    }

    /**
     * 客户端关闭需要释放资源
     **/
    public void close() {
        group.shutdownGracefully();
        log.info("客户端注销成功！");
    }
}
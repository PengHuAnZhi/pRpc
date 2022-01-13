package com.phz.prpc.netty.server;

import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.spring.SpringBeanUtil;
import com.phz.prpc.netty.protocol.MessageCodecSharable;
import com.phz.prpc.netty.protocol.ProtocolFrameDecoder;
import com.phz.prpc.netty.handler.RpcRequestMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * {@code rpc}实例的的{@code Netty}服务端
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月11日 20:17
 */
@Slf4j
public class NettyServer {
    /**
     * 私有构造方法，禁用手动实例化
     **/
    private NettyServer() {
    }

    /**
     * {@code NettyServer}单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link NettyServer#getInstance()}方法，才会加载此内部类，然后创建唯一{@code Netty}服务端
     **/
    private static class NettyServerHolder {
        private static final NettyServer INSTANCE = new NettyServer();
    }

    /**
     * 获取{@link NettyServer}单例对象
     *
     * @return NettyServer {@link NettyServer}单例对象
     **/
    public static NettyServer getInstance() {
        return NettyServerHolder.INSTANCE;
    }

    /**
     * 扫描到服务提供对象后，就会开启{@code Netty}服务
     **/
    @SneakyThrows
    public void start() {
        PrpcProperties prpcProperties = SpringBeanUtil.getBean(PrpcProperties.class);
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
        MessageCodecSharable messageCodecSharable = new MessageCodecSharable();
        // rpc 请求消息处理器
        RpcRequestMessageHandler rpcRequestMessageHandler = new RpcRequestMessageHandler();
        ChannelFuture channelFuture = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(loggingHandler);
                        ch.pipeline().addLast(new ProtocolFrameDecoder());
                        ch.pipeline().addLast(messageCodecSharable);
                        ch.pipeline().addLast(rpcRequestMessageHandler);
                    }
                }).bind(prpcProperties.getPort()).sync();
        ChannelFuture closeFuture = channelFuture.channel().closeFuture();
        closeFuture.addListener((ChannelFutureListener) future -> {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        });
    }
}
package com.phz.prpc.netty.server;

import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.netty.handler.RpcRequestMessageHandler;
import com.phz.prpc.netty.protocol.MessageCodecSharable;
import com.phz.prpc.netty.protocol.ProtocolFrameDecoder;
import com.phz.prpc.spring.SpringBeanUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
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
public final class NettyServer {
    /**
     * 私有构造方法，禁用手动实例化
     **/
    private NettyServer() {
    }

    /**
     * {@code NettyServer}单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link NettyServer#getInstance()}方法，才会加载此内部类，然后创建唯一{@code Netty}服务端
     **/
    private static class NettyServerHolder {
        /**
         * 单例
         **/
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
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
        MessageCodecSharable messageCodecSharable = new MessageCodecSharable();
        // rpc 请求消息处理器
        RpcRequestMessageHandler rpcRequestMessageHandler = new RpcRequestMessageHandler();
        ChannelFuture channelFuture = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        /*
                         * 5s 内如果没有收到 channel 的数据，会触发一个 IdleState#READER_IDLE 事件
                         * readerIdleTimeSeconds：读的空闲时间上限
                         * writerIdleTimeSeconds：写的空闲时间上限
                         * allIdleTimeSeconds：读写都空闲的时间上限
                         **/
                        ch.pipeline().addLast(new IdleStateHandler(5, 0, 0));
                        // ChannelDuplexHandler 可以同时作为入站和出站处理器
                        ch.pipeline().addLast(new ChannelDuplexHandler() {
                            /**
                             * 用来触发特殊事件
                             **/
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                if (evt instanceof IdleStateEvent) {
                                    IdleStateEvent event = (IdleStateEvent) evt;
                                    // 触发了读空闲事件
                                    if (event.state() == IdleState.READER_IDLE) {
                                        log.debug("已经 5s 没有读到数据了");
                                        if (!ctx.channel().isActive()) {
                                            ctx.channel().close();
                                        }
                                    }
                                }
                            }

                            /**
                             * 用来触发异常事件
                             **/
                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                log.info("发现异常 : {}{} 连接关闭...", cause.getMessage(), ctx.channel().remoteAddress());
                                ctx.channel().close();
                            }
                        });
                        ch.pipeline().addLast(loggingHandler);
                        ch.pipeline().addLast(new ProtocolFrameDecoder());
                        ch.pipeline().addLast(messageCodecSharable);
                        ch.pipeline().addLast(rpcRequestMessageHandler);
                    }
                }).bind(prpcProperties.getServerPort()).sync();
        ChannelFuture closeFuture = channelFuture.channel().closeFuture();
        closeFuture.addListener((ChannelFutureListener) future -> {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        });
    }
}
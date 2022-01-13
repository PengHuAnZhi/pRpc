package com.phz.prpc.netty.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * <p>
 * 由于我们定义好的协议是不会再修改的，所以我们将创建帧解码器的过程简化一下，这样下次创建这个处理器的时候就不用再传入那么多的参数了
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月11日 12:23
 */
public class ProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * 无参构造方法，直接初始化好帧解码器{@link LengthFieldBasedFrameDecoder}的各种参数
     **/
    public ProtocolFrameDecoder() {
        this(4096, 12, 4, 0, 0);
    }

    /**
     * 带参构造方法，也可以指定若干参数信息，然后初始化{@link LengthFieldBasedFrameDecoder}
     *
     * @param maxFrameLength      帧的最大长度
     * @param lengthFieldOffset   长度字段偏移量
     * @param lengthFieldLength   长度字段长度
     * @param lengthAdjustment    长度字段为基准，还有几个字节是内容
     * @param initialBytesToStrip 从头剥离几个字节
     **/
    public ProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
}
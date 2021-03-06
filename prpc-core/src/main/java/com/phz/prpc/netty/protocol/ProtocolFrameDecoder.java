package com.phz.prpc.netty.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * <p>
 * 对于解决粘包半包问题，在我们自定义的协议里面采用了预设长度的的方式解决，对于预设长度的方式，需要借助{@code Netty}给我们提供的预设长度解码处理器{@link LengthFieldBasedFrameDecoder}
 * <br></br> <br></br>
 * 由于我们定义好的协议是不会再修改的，而创建{@link LengthFieldBasedFrameDecoder}需要传的几个参数比较多，所以我们将创建帧解码器的过程简化一下，这样下次创建这个处理器的时候就不用再传入那么多的参数了
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月11日 12:23
 */
public class ProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {
    /**
     * 最长帧
     **/
    private static final Integer MAX_FRAME_LENGTH = 4096;
    /**
     * 长度字段偏移量，在编解码器中，我们将长度字节放在了第四十四个，所以长度字段偏移量为44
     **/
    private static final Integer LENGTH_FIELD_OFFSET = 44;
    /**
     * 长度字段长度
     **/
    private static final Integer LENGTH_FIELD_LENGTH = 4;
    /**
     * 长度字段为基准，还有几个字节是内容
     **/
    private static final Integer LENGTH_ADJUSTMENT = 0;
    /**
     * 从头剥离几个字节
     **/
    private static final Integer INITIAL_BYTES_TO_STRIP = 0;

    /**
     * 无参构造方法，直接初始化好帧解码器{@link LengthFieldBasedFrameDecoder}的各种参数
     **/
    public ProtocolFrameDecoder() {
        this(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
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
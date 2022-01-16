package com.phz.prpc.netty.protocol;

import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.netty.compress.CompressAlgorithm;
import com.phz.prpc.netty.message.Message;
import com.phz.prpc.netty.serializer.SerializerAlgorithm;
import com.phz.prpc.spring.SpringBeanUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 编解码器，用于将{@code rpc}消息按照我们自己的协议编解码
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 21:41
 */
@Slf4j
@ChannelHandler.Sharable
@Component
@DependsOn("prpcProperties")
public class MessageCodecSharable extends MessageToMessageCodec<ByteBuf, Message> {
    /**
     * 读取配置文件的类{@link PrpcProperties}
     **/
    private static final PrpcProperties PRPC_PROPERTIES = SpringBeanUtil.getBean(PrpcProperties.class);

    /**
     * 魔数，判断是否无效数据包
     **/
    private static final byte[] MAGIC_NUMBER = new byte[]{1, 2, 3, 4};

    /**
     * 请求序列号{@link Message#getSequenceId()}字节数组长度
     **/
    private static final int SEQUENCE_ID_LENGTH = 36;

    /**
     * 版本号
     **/
    private static final byte VERSION = 1;

    /**
     * 填充字节长度，满足固定字节长度为2^n
     **/
    private static final int FILL_BYTE_LENGTH = 16;

    /**
     * 将明文按照自己的协议编码
     *
     * @param ctx     {@link ChannelHandlerContext}处理器上下文
     * @param msg     {@link Message} 消息的抽象类，也就是可以对{@code rpc}响应和请求类型的数据编码
     * @param outList {@link List<Object>} 将编码后的消息放入消息集合中，等待消息处理器链传递处理
     **/
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> outList) {
        String serializer = PRPC_PROPERTIES.getSerializerAlgorithm();
        SerializerAlgorithm serializerAlgorithm;
        try {
            serializerAlgorithm = SerializerAlgorithm.valueOf(serializer.toUpperCase());
            log.info("{} 发送消息的序列化算法为:{}", ctx.channel().localAddress(), serializer);
        } catch (IllegalArgumentException e) {
            log.error("未知的序列化算法:{},异常信息为:{}", serializer, e.getMessage());
            throw new PrpcException(ErrorMsg.UNKNOWN_SERIALIZER_ALGORITHM);
        }
        int serializerOrdinal = serializerAlgorithm.ordinal();
        ByteBuf out = ctx.alloc().buffer();
        // 1. 4 字节的魔数
        out.writeBytes(MAGIC_NUMBER);
        // 2. 1 字节的版本
        out.writeByte(VERSION);
        // 3. 1 字节的序列化方式 枚举类ordinal()方法可以获取下标，也是从0开始的
        out.writeByte(serializerOrdinal);
        // 4. 1 字节的指令类型
        int messageType = msg.getMessageType();
        out.writeByte(messageType);
        // 5. 36个字节的请求序列号
        byte[] sequenceId = msg.getSequenceId().getBytes(StandardCharsets.UTF_8);
        out.writeBytes(sequenceId);
        // 6、无意义，对齐填充
        out.writeBytes(new byte[FILL_BYTE_LENGTH]);
        // 7. 根据指定的序列化方式去序列化
        byte[] message = serializerAlgorithm.serialize(msg);
        // 8.内容压缩算法
        String compress = PRPC_PROPERTIES.getCompressAlgorithm();
        CompressAlgorithm compressAlgorithm;
        try {
            compressAlgorithm = CompressAlgorithm.valueOf(compress.toUpperCase());
            log.info("{} 发送消息的压缩算法为:{}", ctx.channel().localAddress(), compress);
        } catch (IllegalArgumentException e) {
            log.error("未知的消息压缩算法:{},异常信息为:{}", compress, e.getMessage());
            throw new PrpcException(ErrorMsg.UNKNOWN_COMPRESS_ALGORITHM);
        }
        int compressOrdinal = compressAlgorithm.ordinal();
        out.writeByte(compressOrdinal);
        byte[] compressMessage = compressAlgorithm.compress(message);
        // 9. 长度
        int length = compressMessage.length;
        out.writeInt(length);
        // 10. 写入内容
        out.writeBytes(compressMessage);
        log.info("编码：magicNum:{}, version:{}, serializer:{}, messageType:{}, sequenceId:{}, length:{}, compressAlgorithm:{}", MAGIC_NUMBER, VERSION, serializerAlgorithm, messageType, sequenceId, length, compressAlgorithm);
        outList.add(out);
    }

    /**
     * 将明文按照自己的协议解码
     *
     * @param ctx     {@link ChannelHandlerContext}处理器上下文
     * @param in      {@link ByteBuf} 消息的抽象类，也就是可以对{@code rpc}响应和请求类型的数据解码
     * @param outList {@link List<Object>} 将解码后的消息放入消息集合中，等待消息处理器链传递处理
     **/
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> outList) {
        ByteBuf unwrap = in.unwrap();
        unwrap.resetReaderIndex();
        byte[] magicNum = new byte[MAGIC_NUMBER.length];
        // 1. 4 字节的魔数
        unwrap.readBytes(magicNum);
        if (!Arrays.equals(magicNum, MAGIC_NUMBER)) {
            log.error("未知的魔数:{}", Arrays.toString(magicNum));
            throw new PrpcException(ErrorMsg.UNKNOWN_MAGIC_CODE);
        }
        // 2. 1 字节的版本号
        byte version = unwrap.readByte();
        // 3. 1 字节的序列化算法
        byte serializer = unwrap.readByte();
        // 4. 1 字节的指令类型
        byte messageType = unwrap.readByte();
        // 5. 36个字节的请求序列号
        byte[] sequenceIdBytes = new byte[SEQUENCE_ID_LENGTH];
        unwrap.readBytes(sequenceIdBytes);
        String sequenceId = new String(sequenceIdBytes, (StandardCharsets.UTF_8));
        // 6、无意义，对齐填充
        unwrap.readBytes(new byte[FILL_BYTE_LENGTH]);
        byte compress = unwrap.readByte();
        // 7. 长度
        int length = unwrap.readInt();
        // 8. 读取内容
        byte[] compressMessage = new byte[length];
        unwrap.readBytes(compressMessage, 0, length);
        // 找到压缩算法
        CompressAlgorithm compressAlgorithm;
        try {
            compressAlgorithm = CompressAlgorithm.values()[compress];
        } catch (IllegalArgumentException e) {
            log.error("{}收到的消息所指定压缩算法未知,异常信息为:{}", ctx.channel().localAddress(), e.getMessage());
            throw new PrpcException(ErrorMsg.UNKNOWN_COMPRESS_ALGORITHM);
        }
        byte[] decompressMessage = compressAlgorithm.decompress(compressMessage);
        // 找到反序列化算法
        SerializerAlgorithm serializerAlgorithm;
        try {
            serializerAlgorithm = SerializerAlgorithm.values()[serializer];
        } catch (IllegalArgumentException e) {
            log.error("{}收到的消息所指定反序列化算法未知,异常信息为:{}", ctx.channel().localAddress(), e.getMessage());
            throw new PrpcException(ErrorMsg.UNKNOWN_SERIALIZER_ALGORITHM);
        }
        // 确定具体消息类型
        Class<? extends Message> messageClass = Message.getMessageClass(messageType);
        Message message = (Message) serializerAlgorithm.deserialize(messageClass, decompressMessage);
        log.info("解码：magicNum:{}, version:{}, serializerAlgorithm:{}, messageType:{}, sequenceId:{}, length:{}, compressAlgorithm:{}", magicNum, version, serializerAlgorithm, messageType, sequenceId, length, compressAlgorithm);
        outList.add(message);
    }
}
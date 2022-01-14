package com.phz.prpc.netty.protocol;

import com.phz.prpc.config.PrpcProperties;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import com.phz.prpc.netty.message.Message;
import com.phz.prpc.netty.serializer.SerializerAlgorithm;
import com.phz.prpc.spring.SpringBeanUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.internal.StringUtil;
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
    private final PrpcProperties prpcProperties = SpringBeanUtil.getBean(PrpcProperties.class);

    /**
     * 默认序列化算法为JSON
     **/
    private static final String DEFAULT_SERIALIZER_ALGORITHM = "JSON";

    /**
     * 魔数，判断是否无效数据包
     **/
    private static final byte[] MAGIC_NUMBER = new byte[]{1, 2, 3, 4};

    /**
     * 请求序列号{@link Message#sequenceId}字节数组长度
     **/
    private static final int SEQUENCE_ID_LENGTH = 36;

    /**
     * 将明文按照自己的协议编码
     *
     * @param ctx     {@link ChannelHandlerContext}处理器上下文
     * @param msg     {@link Message} 消息的抽象类，也就是可以对{@code rpc}响应和请求类型的数据编码
     * @param outList {@link List<Object>} 将编码后的消息放入消息集合中，等待消息处理器链传递处理
     **/
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> outList) {
        String serializerAlgorithm = prpcProperties.getSerializerAlgorithm();
        if (StringUtil.isNullOrEmpty(serializerAlgorithm)) {
            serializerAlgorithm = DEFAULT_SERIALIZER_ALGORITHM;
        }
        SerializerAlgorithm algorithm;
        try {
            algorithm = SerializerAlgorithm.valueOf(serializerAlgorithm);
            log.info("{} 发送消息的序列化算法为:{}", ctx.channel().localAddress(), serializerAlgorithm);
        } catch (IllegalArgumentException e) {
            log.error("未知的序列化算法:{},异常信息为:{}", serializerAlgorithm, e.getMessage());
            throw new PrpcException(ErrorMsg.UNKNOWN_SERIALIZER_ALGORITHM);
        }
        //默认JSON
        int ordinal = algorithm.ordinal();
        ByteBuf out = ctx.alloc().buffer();
        // 1. 4 字节的魔数
        out.writeBytes(MAGIC_NUMBER);
        // 2. 1 字节的版本
        out.writeByte(1);
        // 3. 1 字节的序列化方式 枚举类ordinal()方法可以获取下标，也是从0开始的
        out.writeByte(ordinal);
        // 4. 1 字节的指令类型
        out.writeByte(msg.getMessageType());
        // 5. 36个字节的请求序列号
        out.writeBytes(msg.getSequenceId().getBytes(StandardCharsets.UTF_8));
        // 6、无意义，对齐填充
        out.writeByte(0);
        // 7. 根据指定的序列化方式去序列化
        byte[] message = algorithm.serialize(msg);
        // 8. 长度
        out.writeInt(message.length);
        // 9. 写入内容
        out.writeBytes(message);
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
        byte[] magicNum = new byte[MAGIC_NUMBER.length];
        // 1. 4 字节的魔数
        in.readBytes(magicNum);
        if (!Arrays.equals(magicNum, MAGIC_NUMBER)) {
            log.error("未知的魔数:{}", Arrays.toString(magicNum));
            throw new PrpcException(ErrorMsg.UNKNOWN_MAGIC_CODE);
        }
        // 2. 1 字节的版本号
        byte version = in.readByte();
        // 3. 1 字节的序列化算法
        byte serializerAlgorithm = in.readByte();
        // 4. 1 字节的指令类型
        byte messageType = in.readByte();
        // 5. 36个字节的请求序列号
        byte[] sequenceIdBytes = new byte[SEQUENCE_ID_LENGTH];
        String sequenceId = in.readBytes(sequenceIdBytes).toString(StandardCharsets.UTF_8);
        // 6、无意义，对齐填充
        in.readByte();
        // 7. 长度
        int length = in.readInt();
        // 8. 读取内容
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        // 找到反序列化算法
        SerializerAlgorithm algorithm;
        try {
            algorithm = SerializerAlgorithm.values()[serializerAlgorithm];
        } catch (IllegalArgumentException e) {
            log.error("{}收到的消息所指定反序列化算法未知,异常信息为:{}", ctx.channel().localAddress(), e.getMessage());
            throw new PrpcException(ErrorMsg.UNKNOWN_SERIALIZER_ALGORITHM);
        }
        // 确定具体消息类型
        Class<? extends Message> messageClass = Message.getMessageClass(messageType);
        Message message = (Message) algorithm.deserialize(messageClass, bytes);
        log.info("magicNum:{}, version:{}, serializerAlgorithm:{}, messageType:{}, sequenceId:{}, length:{}", magicNum, version, serializerAlgorithm, messageType, sequenceId, length);
        outList.add(message);
    }
}
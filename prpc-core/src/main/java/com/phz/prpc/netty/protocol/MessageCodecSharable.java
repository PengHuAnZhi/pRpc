package com.phz.prpc.netty.protocol;

import com.phz.prpc.config.PrpcProperties;
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
     * 将明文按照自己的协议编码
     *
     * @param ctx     {@link ChannelHandlerContext}处理器上下文
     * @param msg     {@link Message} 消息的抽象类，也就是可以对{@code rpc}响应和请求类型的数据编码
     * @param outList {@link List<Object>} 将编码后的消息放入消息集合中，等待消息处理器链传递处理
     **/
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> outList) {
        ByteBuf out = ctx.alloc().buffer();
        // 1. 4 字节的魔数
        out.writeBytes(new byte[]{1, 2, 3, 4});
        // 2. 1 字节的版本
        out.writeByte(1);
        // 3. 1 字节的序列化方式 枚举类ordinal()方法可以获取下标，也是从0开始的
        out.writeByte(SerializerAlgorithm.valueOf(prpcProperties.getSerializerAlgorithm()).ordinal());
        // 4. 1 字节的指令类型
        out.writeByte(msg.getMessageType());
        // 5. 4 个字节
        out.writeInt(msg.getSequenceId());
        // 6、无意义，对齐填充
        out.writeByte(0);
        // 7. 根据指定的序列化方式去序列化
        byte[] message = SerializerAlgorithm.valueOf(prpcProperties.getSerializerAlgorithm()).serialize(msg);
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
        // 1. 4 字节的魔数
        int magicNum = in.readInt();
        // 2. 1 字节的版本号
        byte version = in.readByte();
        // 3. 1 字节的序列化算法
        byte serializerAlgorithm = in.readByte();
        // 4. 1 字节的指令类型
        byte messageType = in.readByte();
        // 5. 4 个字节的请求序号
        int sequenceId = in.readInt();
        // 6、无意义，对齐填充
        in.readByte();
        // 7. 长度
        int length = in.readInt();
        // 8. 读取内容
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        // 找到反序列化算法
        SerializerAlgorithm algorithm = SerializerAlgorithm.values()[serializerAlgorithm];
        // 确定具体消息类型
        Class<? extends Message> messageClass = Message.getMessageClass(messageType);
        Message message = (Message) algorithm.deserialize(messageClass, bytes);
        log.info("magicNum:{}, version:{}, serializerAlgorithm:{}, messageType:{}, sequenceId:{}, length:{}", magicNum, version, serializerAlgorithm, messageType, sequenceId, length);
        outList.add(message);
    }
}
package com.phz.prpc.netty.serializer;

import com.alibaba.nacos.common.codec.Base64;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * {@link com.esotericsoftware.kryo.Kryo}序列化实现类
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月15日 20:07
 */
@Slf4j
public class KryoSerializer {
    /**
     * 因为Kryo不是线程安全的。所以使用线程本地存储每个线程的{@link Kryo}实例
     **/
    private static final ThreadLocal<Kryo> KRYO_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        /*
         * 不要轻易改变这里的配置！更改之后，序列化的格式就会发生变化，
         * 上线的同时就必须清除 Redis 里的所有缓存，
         * 否则那些缓存再回来反序列化的时候，就会报错
         *
         * 支持对象循环引用（否则会栈溢出）
         * 默认值就是 true，添加此行的目的是为了提醒维护者，不要改变这个配置
         */
        kryo.setReferences(true);
        /*
         * 不强制要求注册类（注册行为无法保证多个 JVM 内同一个类的注册编号相同；而且业务系统中大量的 Class 也难以一一注册）
         * 默认值就是 false，添加此行的目的是为了提醒维护者，不要改变这个配置
         **/
        kryo.setRegistrationRequired(false);
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy()).setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    });

    /**
     * 获得当前线程的  {@link Kryo} 实例
     *
     * @return 当前线程的 {@link Kryo} 实例
     */
    public static Kryo getInstance() {
        return KRYO_LOCAL.get();
    }

    /**
     * 将对象【及类型】序列化为字节数组
     *
     * @param obj 任意对象
     * @return 序列化后的字节数组
     */
    public static byte[] serialize(Object obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        Kryo kryo = getInstance();
        //Object->byte:将对象序列化为byte数组
        kryo.writeClassAndObject(output, obj);
        output.flush();
        byte[] bytes = byteArrayOutputStream.toByteArray();
        try {
            output.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            log.error("Kryo资源释放失败:{}", e.getMessage());
            return bytes;
        }
        return bytes;
    }

    /**
     * 将对象【及类型】序列化为 {@link String}
     * 利用了 {@link Base64} 编码
     *
     * @param obj 任意对象
     * @return 序列化后的字符串
     */
    public static String writeToString(Object obj) {
        return new String(Base64.encodeBase64(serialize(obj)), StandardCharsets.UTF_8);
    }

    /**
     * 将字节数组反序列化为原对象
     *
     * @param byteArray writeToByteArray 方法序列化后的字节数组
     * @return 原对象
     */
    public static Object deserialize(byte[] byteArray) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        Input input = new Input(byteArrayInputStream);
        Kryo kryo = getInstance();
        //byte->Object:从byte数组中反序列化出对对象
        Object object = kryo.readClassAndObject(input);
        try {
            input.close();
            byteArrayInputStream.close();
        } catch (IOException e) {
            log.error("Kryo资源释放失败:{}", e.getMessage());
            return object;
        }
        return object;
    }

    /**
     * 将 {@link String} 反序列化为原对象
     * 利用了 {@link Base64} 编码
     *
     * @param str {@link KryoSerializer#writeToString} 方法序列化后的字符串
     * @return 原对象
     */
    public static Object readFromString(String str) {
        return deserialize(Base64.decodeBase64(str.getBytes(StandardCharsets.UTF_8)));
    }
}
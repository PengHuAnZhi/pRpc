package com.phz.prpc.netty.serializer;

/**
 * <p>
 * 序列化的接口，所有{@code prpc}的序列化方法都需要实现这个接口
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 21:40
 */
public interface Serializer {
    /**
     * 反序列化方法
     *
     * @param <T>   原始对象泛型
     * @param clazz 因为{@code JDK}序列化出来的字节数组包含了原始对象信息，而其他的序列化算法便不都包含了，所以需要指定反序列化出来的对象是谁
     * @param bytes 字节数组
     * @return T 返回指定对象
     */
    <T> Object deserialize(Class<T> clazz, byte[] bytes);

    /**
     * 序列化方法
     *
     * @param <T>    原始对象泛型
     * @param object 待序列化对象
     * @return byte[] 返回序列化后的字节数组
     */
    <T> byte[] serialize(T object);
}
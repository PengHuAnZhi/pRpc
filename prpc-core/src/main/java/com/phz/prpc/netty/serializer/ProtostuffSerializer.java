package com.phz.prpc.netty.serializer;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code protostuff}序列化实现类
 *
 * @author PengHuanZhi
 * @date 2022年01月16日 9:31
 */
public class ProtostuffSerializer {

    /**
     * 避免每次序列化都重新申请{@code Buffer}空间
     **/
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
    /**
     * 缓存{@link Schema}
     **/
    private static final Map<Class<?>, Schema<?>> SCHEMA_CACHE = new ConcurrentHashMap<>();

    /**
     * 序列化方法，把指定对象序列化成字节数组
     *
     * @param obj 待序列化对象
     * @return byte 序列化后的字节数组
     **/
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj) {
        Class<T> clazz = (Class<T>) obj.getClass();
        Schema<T> schema = getSchema(clazz);
        byte[] data;
        try {
            data = ProtostuffIOUtil.toByteArray(obj, schema, BUFFER);
        } finally {
            BUFFER.clear();
        }
        return data;
    }

    /**
     * 反序列化方法，将字节数组反序列化成指定{@link Class}类型
     *
     * @param data  待反序列化字节数组
     * @param clazz 需要反序列化出来的对象{@link Class}
     * @return T 返回指定反序列化后的对象
     **/

    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        Schema<T> schema = getSchema(clazz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(data, obj, schema);
        return obj;
    }

    /**
     * 获取并缓存{@link Schema}
     *
     * @param clazz 对象的{@link Class}
     * @return Schema<T> 返回与之匹配的{@link Schema}
     **/
    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) SCHEMA_CACHE.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.getSchema(clazz);
            if (schema == null) {
                SCHEMA_CACHE.put(clazz, schema);
            }
        }
        return schema;
    }
}
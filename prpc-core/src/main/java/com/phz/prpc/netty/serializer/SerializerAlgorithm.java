package com.phz.prpc.netty.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 序列化算法的实现枚举
 *
 * @author PengHuanZhi
 * @date 2022年01月13日 12:24
 */
public enum SerializerAlgorithm implements Serializer {
    /**
     * {@code JDK}序列化实现
     **/
    JDK {
        /**
         * 反序列化方法
         *
         * @param clazz 因为{@code JDK}序列化出来的字节数组包含了原始对象信息，而其他的序列化算法便不都包含了，所以需要指定反序列化出来的对象是谁
         * @param bytes 字节数组
         * @return T 返回指定对象
         */
        @Override
        public <T> Object deserialize(Class<T> clazz, byte[] bytes) {
            try {
                ObjectInputStream in =
                        new ObjectInputStream(new ByteArrayInputStream(bytes));
                return in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("SerializerAlgorithm.JDK 反序列化错误", e);
            }
        }

        /**
         * 序列化方法
         *
         * @param object 待序列化对象
         * @return byte[] 返回序列化后的字节数组
         */
        @Override
        public <T> byte[] serialize(T object) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                new ObjectOutputStream(out).writeObject(object);
                return out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("SerializerAlgorithm.JDK 序列化错误", e);
            }
        }
    },
    /**
     * {@code JSON}序列化实现
     **/
    JSON {
        /**
         * 反序列化方法
         *
         * @param clazz 因为{@code JDK}序列化出来的字节数组包含了原始对象信息，而其他的序列化算法便不都包含了，所以需要指定反序列化出来的对象是谁
         * @param bytes 字节数组
         * @return T 返回指定对象
         */
        @Override
        public <T> T deserialize(Class<T> clazz, byte[] bytes) {
            Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new JsonClassCodec()).create();
            return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), clazz);
        }

        /**
         * 序列化方法
         *
         * @param object 待序列化对象
         * @return byte[] 返回序列化后的字节数组
         */
        @Override
        public <T> byte[] serialize(T object) {
            Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new JsonClassCodec()).create();
            return gson.toJson(object).getBytes(StandardCharsets.UTF_8);
        }
    }
}
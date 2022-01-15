package com.phz.prpc.netty.serializer;

import com.alibaba.fastjson.JSON;
import com.google.gson.*;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 序列化算法的实现枚举
 *
 * @author PengHuanZhi
 * @date 2022年01月13日 12:24
 */
@Slf4j
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
                log.error("JDK 反序列化失败 : {}", e.getMessage());
                throw new PrpcException(ErrorMsg.DESERIALIZE_FAILED);
            }
        }
    },
    /**
     * {@code JSON}序列化实现
     * <p><br></br>
     * {@link Gson}序列化{@code Java}代码的时候，没有提供正确的类型转换器，需要我们自己实现
     * </p>
     * <br></br>
     **/
    GSON {
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

        class JsonClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

            /**
             * 反序列化方法
             *
             * @param json    被反序列化{@code Json}数据
             * @param typeOfT 反序列化的对象的类型
             * @param context 反序列化器的上下文
             * @return Class<?> 反序列化对象的指定类型的类型的一个子类
             **/
            @Override
            public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                try {
                    return Class.forName(json.getAsString());
                } catch (ClassNotFoundException e) {
                    log.error("Json反序列化失败 : {}", e.getMessage());
                    throw new PrpcException(ErrorMsg.DESERIALIZE_FAILED);
                }
            }

            /**
             * 序列化方法
             *
             * @param src       需要转换为{@code Json}的对象
             * @param typeOfSrc 序列化的对象的类型
             * @param context   序列化器的上下文
             * @return JsonElement 被序列化{@code Json}数据
             **/
            @Override
            public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
                // 将传递进来的Class的全路径转换为Class，在传输的时候只需要知道类的全路径就可以啦
                return new JsonPrimitive(src.getName());
            }
        }
    },
    FASTJSON {
        @Override
        public <T> Object deserialize(Class<T> clazz, byte[] bytes) {
            return JSON.parseObject(bytes, clazz);
        }

        @Override
        public <T> byte[] serialize(T object) {
            return JSON.toJSONBytes(object);
        }
    },
    KYRO {
        @Override
        public <T> Object deserialize(Class<T> clazz, byte[] bytes) {
            return KryoSerializer.readFromByteArray(bytes);
        }

        @Override
        public <T> byte[] serialize(T object) {
            return KryoSerializer.writeToByteArray(object);
        }
    }
}
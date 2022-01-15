package com.phz.prpc.netty.serializer;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
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
     * {@code GSON}序列化实现
     **/
    GSON {
        @Override
        public <T> T deserialize(Class<T> clazz, byte[] bytes) {
            Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, GsonSerializer.getInstance()).create();
            return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), clazz);
        }

        @Override
        public <T> byte[] serialize(T object) {
            Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, GsonSerializer.getInstance()).create();
            return gson.toJson(object).getBytes(StandardCharsets.UTF_8);
        }
    },
    /**
     * {@code FASTJSON}序列化实现
     **/
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
    /**
     * {@code KYRO}序列化实现
     **/
    KYRO {
        @Override
        public <T> Object deserialize(Class<T> clazz, byte[] bytes) {
            return KryoSerializer.readFromByteArray(bytes);
        }

        @Override
        public <T> byte[] serialize(T object) {
            return KryoSerializer.writeToByteArray(object);
        }
    },
    /**
     * {@code HESSIAN}序列化实现
     **/
    HESSIAN {
        @Override
        public <T> Object deserialize(Class<T> clazz, byte[] bytes) {
            return Hessian2Serializer.deserialize(clazz, bytes);
        }

        @Override
        public <T> byte[] serialize(T object) {
            return Hessian2Serializer.serialize(object);
        }
    }
}
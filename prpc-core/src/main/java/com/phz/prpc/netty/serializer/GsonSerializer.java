package com.phz.prpc.netty.serializer;

import com.google.gson.*;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;

/**
 * {@link Gson}序列化{@code Java}代码的时候，没有提供正确的类型转换器，需要我们自己实现
 *
 * @author PengHuanZhi
 * @date 2022年01月15日 21:55
 */
@Slf4j
public class GsonSerializer implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

    /**
     * 私有构造方法，禁用手动实例化
     **/
    private GsonSerializer() {
    }

    /**
     * {@code GsonSerializer}单例维护静态内部类：类的加载都是懒惰的，第一次调用{@link GsonSerializer#getInstance()}方法，才会加载此内部类，然后创建唯一{@code GsonSerializer}
     **/
    private static class GsonSerializerHolder {
        private static final GsonSerializer INSTANCE = new GsonSerializer();
    }

    /**
     * 获取{@link GsonSerializer}单例对象
     *
     * @return GsonSerializer {@link GsonSerializer}单例对象
     **/
    public static GsonSerializer getInstance() {
        return GsonSerializerHolder.INSTANCE;
    }

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
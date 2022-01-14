package com.phz.prpc.netty.serializer;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * <p>
 * {@link Gson}序列化{@code Java}代码的时候，没有提供正确的类型转换器，需要我们自己实现。
 * </p>
 * <br></br>
 * <p>
 * 因为需要将{@link Class}和{@code json}互转，所以这里的泛型就用{@link Class}
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月13日 12:55
 */
public class JsonClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

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
            String str = json.getAsString();
            return Class.forName(str);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
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
package com.phz.prpc.netty.serializer;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.phz.prpc.exception.ErrorMsg;
import com.phz.prpc.exception.PrpcException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <p>
 * {@code Hessian2}序列化实现类
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月15日 21:39
 */
@Slf4j
public class Hessian2Serializer {
    /**
     * {@code JavaBean}序列化.
     *
     * @param javaBean {@code Java}对象.
     */
    public static <T> byte[] serialize(T javaBean) {
        Hessian2Output ho = null;
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            ho = new Hessian2Output(byteArrayOutputStream);
            ho.writeObject(javaBean);
            ho.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception ex) {
            throw new PrpcException(ErrorMsg.HESSIAN_SERIALIZE_FAILED);
        } finally {
            if (null != ho) {
                try {
                    ho.close();
                } catch (IOException e) {
                    log.error("Hessian2Output资源释放失败:{}", e.getMessage());
                }
            }
        }
    }

    /**
     * JavaBean反序列化.
     *
     * @param clazz         反序列化对象的{@link Class}
     * @param serializeData 序列化数据.
     */
    public static <T> T deserialize(Class<T> clazz, byte[] serializeData) {
        Hessian2Input hi = null;
        ByteArrayInputStream byteArrayInputStream;
        try {
            byteArrayInputStream = new ByteArrayInputStream(serializeData);
            hi = new Hessian2Input(byteArrayInputStream);
            return clazz.cast(hi.readObject());
        } catch (Exception ex) {
            throw new PrpcException(ErrorMsg.HESSIAN_DESERIALIZE_FAILED);
        } finally {
            if (null != hi) {
                try {
                    hi.close();
                } catch (IOException e) {
                    log.error("Hessian2Input资源释放失败:{}", e.getMessage());
                }
            }
        }
    }
}
package com.phz.prpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 加入{@code Spring}容器管理，这样被{@link PrpcServer}注解的类就会交由 {@code Spring} 管理
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 12:21
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface PrpcServer {
    /**
     * 同一个服务可能有多种实现，配置当前服务的组名以示区分
     **/
    String groupName();
}
package com.phz.prpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * <p>
 * 包含一个服务名称的属性,用于指定远程调用的服务名称，并加入{@code Spring}容器管理，这样被{@link PrpcClient}注解的类就会交由 {@code Spring} 管理
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 14:13
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface PrpcClient {
    /**
     * 同一个服务可能有多种实现，指定当前服务的组名以示区分
     **/
    String groupName();
}
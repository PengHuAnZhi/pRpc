package com.phz.prpc.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * <p>
 * 当{@code Spring}容器初始化的时候，会自动的调用{@link SpringBeanUtil#setApplicationContext}方法将{@link ApplicationContext}注入进来，从而能够直接通过容器获取我们需要的对象
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月10日 9:49
 */
@Component
public class SpringBeanUtil implements ApplicationContextAware {
    /**
     * {@link ApplicationContext}容器上下文对象
     **/
    private static ApplicationContext applicationContext;

    /**
     * 当{@code Spring}容器初始化的后，自动调用当前方法，将{@link ApplicationContext}容器上下文对象传递进来
     *
     * @param applicationContext {@link ApplicationContext}容器上下文对象
     **/
    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        SpringBeanUtil.applicationContext = applicationContext;
    }

    /**
     * 通过{@code Bean}名称从容器获取{@code Bean}
     *
     * @param name {@code Bean}名称
     * @return Object 返回目标{@code Bean}
     **/
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    /**
     * 通过{@code Bean}名称以及{@link Class}对象从容器获取{@code Bean}
     *
     * @param <T>   目标{@code Bean}的类型
     * @param name  {@code Bean}名称
     * @param clazz 目标{@code Bean}的{@link Class}对象
     * @return T 返回目标{@code Bean}
     **/
    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 通过{@link Class}对象从容器中获取{@code Bean}
     *
     * @param <T>   目标{@code Bean}的类型
     * @param clazz 目标{@code Bean}的{@link Class}对象
     * @return T 返回目标{@code Bean}
     **/
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
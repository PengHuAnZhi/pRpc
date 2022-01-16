package com.phz.prpc.extension;

import java.lang.annotation.*;

/**
 * <p>
 * {@code SPI}注解，可运行其他第三方实现的抽象接口需使用此注解
 * </p>
 * </br>
 * <p>
 * {@code JDK}的{@code SPI}机制是{@code JDK}提供接口，第三方{@code jar}包实现，接口由启动类加载器加载，实现类不在{@code JDK}中,需要反向委派，由线程上下文加载器加载。
 * </p>
 * </br>
 * <p>
 * 它约定：在 {@code jar} 包的 {@code META-INF/services} 包下，以接口全限定名为文件名，文件内容是实现类名称
 * </p>
 * </br>
 * <p>
 * 那么我们完全可以参照它的思想取仿写一个
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月16日 17:50
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Spi {
}
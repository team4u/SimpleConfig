package org.team4u.config;

import java.lang.annotation.*;

/**
 * 配置类注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {

    /**
     * 配置组标识
     */
    String value() default "";
}

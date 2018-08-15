package org.team4u.config;

import java.lang.annotation.*;

/**
 * 忽略配置的字段
 *
 * @author Jay Wu
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreField {
}
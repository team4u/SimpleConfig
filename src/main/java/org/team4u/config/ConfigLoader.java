package org.team4u.config;

import java.io.Closeable;
import java.util.List;

/**
 * 配置加载器
 *
 * @author Jay.Wu
 */
public interface ConfigLoader<C extends SystemConfig> extends Closeable {
    /**
     * 加载所有配置
     */
    List<C> load();

    /**
     * 转换为配置对象，指定前缀
     */
    <T> T to(Class<T> toType, String prefix);

    /**
     * 转换为配置对象，无前缀或者使用ConfigurationProperties注解
     */
    <T> T to(Class<T> toType);
}
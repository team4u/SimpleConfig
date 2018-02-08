package org.team4u.config;

import java.util.Date;

/**
 * 配置类接口
 *
 * @author Jay.Wu
 */
public interface SystemConfig {

    /**
     * 配置名称
     */
    String getName();

    /**
     * 配置值
     */
    String getValue();

    /**
     * 配置组
     */
    String getType();

    /**
     * 配置排序
     */
    int getSequenceNo();

    /**
     * 是否开启配置
     */
    Boolean getEnabled();

    /**
     * 配置描述
     */
    String getDescription();

    /**
     * 配置最后更新时间
     */
    Date getUpdateTime();
}
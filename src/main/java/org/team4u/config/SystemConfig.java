package org.team4u.config;

import java.util.Date;

/**
 * @author Jay.Wu
 */
public interface SystemConfig {

    String getName();

    String getValue();

    String getType();

    int getSequenceNo();

    Boolean getEnabled();

    String getDescription();

    Date getUpdateTime();
}
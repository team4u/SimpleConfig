package org.team4u.config;

/**
 * @author Jay.Wu
 */
public interface SystemConfig<S> {

    String getName();

    S setName(String name);

    String getValue();

    S setValue(String value);

    String getType();

    S setType(String type);

    int getSequenceNo();

    S setSequenceNo(int sequenceNo);

    Boolean getEnabled();

    S setEnabled(Boolean enabled);
}
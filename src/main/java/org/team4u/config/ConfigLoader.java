package org.team4u.config;

import java.util.List;

public interface ConfigLoader<C extends SystemConfig> {

    List<C> load();

    <T> T to(Class<T> toType);
}
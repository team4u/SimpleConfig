package org.team4u.config;

import java.util.List;

public interface ConfigLoader {

    List<SystemConfig> load();

    <T> T to(Class<T> toType);
}
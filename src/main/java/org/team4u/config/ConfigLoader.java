package org.team4u.config;

import java.io.Closeable;
import java.util.List;

public interface ConfigLoader<C extends SystemConfig> extends Closeable {

    List<C> load();

    <T> T to(Class<T> toType);
}
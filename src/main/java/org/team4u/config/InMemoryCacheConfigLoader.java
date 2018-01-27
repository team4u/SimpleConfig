package org.team4u.config;

/**
 * @author Jay.Wu
 */
public class InMemoryCacheConfigLoader<C extends SystemConfig> extends AbstractCacheConfigLoader<C> {


    public InMemoryCacheConfigLoader(AbstractConfigLoader<C> delegateConfigLoader,
                                     int cacheTimeoutMs,
                                     Watcher<C> watcher) {
        super(delegateConfigLoader, watcher);
    }


}

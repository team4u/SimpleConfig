package org.team4u.config;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.team4u.kit.core.action.Function;
import org.team4u.kit.core.topic.Topic;
import org.team4u.kit.core.util.CollectionExUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * @author Jay Wu
 */
public abstract class AbstractCacheConfigLoader<C extends SystemConfig>
        extends AbstractConfigLoader<C>
        implements Closeable {
    private final Log log = LogFactory.get();

    protected List<C> configs;
    protected Object toObject;
    protected AbstractConfigLoader<C> delegateConfigLoader;
    protected Topic<String> changedTopic;
    protected Watcher<C> watcher;

    public AbstractCacheConfigLoader(AbstractConfigLoader<C> delegateConfigLoader,
                                     Watcher<C> watcher) {
        this.delegateConfigLoader = delegateConfigLoader;
        this.watcher = watcher;
    }

    @Override
    public <T> T to(Class<T> toType) {
        synchronized (this) {
            if (toObject == null) {
                beforeFirstLoad();
                refresh(toType);
            } else {
                log.trace("loadAllEnabledConfigs from cache(class={})", toType.getSimpleName());
            }
        }

        //noinspection unchecked
        return (T) toObject;
    }

    @Override
    public List<C> load() {
        return configs;
    }

    @Override
    public void close() throws IOException {
        delegateConfigLoader.close();
    }

    protected void beforeFirstLoad() {

    }

    protected synchronized <T> void refresh(Class<T> configClass) {
        List<C> newConfigs = delegateConfigLoader.load();

        // 如果网络异常等情况无法获取最新的配置项,将不更新本地缓存
        if (newConfigs == null) {
            return;
        }

        if (configs == null || !configs.equals(newConfigs)) {
            if (configs != null) {
                log.debug("Config changed");
            }

            configs = newConfigs;
            toObject = to(configClass);
        }
    }

    protected void refreshAndCompareConfigs() {
        List<C> oldConfigs = configs;
        refresh(toObject.getClass());
        compareConfigs(oldConfigs, configs);
    }

    private void compareConfigs(List<C> oldConfigs, List<C> newConfigs) {
        if (oldConfigs == null || watcher == null) {
            return;
        }

        compareCreatedConfigs(oldConfigs, newConfigs);
        compareDeletedConfigs(oldConfigs, newConfigs);
        compareModifyConfigs(oldConfigs, newConfigs);
    }

    /**
     * 比较新增的配置项
     */
    private void compareCreatedConfigs(List<C> oldConfigs, List<C> newConfigs) {
        for (final C newConfig : newConfigs) {
            if (!CollectionExUtil.any(oldConfigs, new Function<C, Boolean>() {
                @Override
                public Boolean invoke(C oldConfig) {
                    return oldConfig.getName().equals(newConfig.getName()) &&
                            oldConfig.getType().equals(newConfig.getType());
                }
            })) {
                watcher.onCreate(newConfig);
            }
        }
    }

    /**
     * 比较删除的配置项
     */
    private void compareDeletedConfigs(List<C> oldConfigs, List<C> newConfigs) {
        for (final C oldConfig : oldConfigs) {
            if (!CollectionExUtil.any(newConfigs, new Function<C, Boolean>() {
                @Override
                public Boolean invoke(C newConfig) {
                    return oldConfig.getName().equals(newConfig.getName()) &&
                            oldConfig.getType().equals(newConfig.getType());
                }
            })) {
                watcher.onDelete(oldConfig);
            }
        }
    }

    /**
     * 比较修改的配置项
     */
    private void compareModifyConfigs(List<C> oldConfigs, List<C> newConfigs) {
        for (final C oldConfig : oldConfigs) {
            C newConfig = CollectionExUtil.find(newConfigs, new Function<C, Boolean>() {
                @Override
                public Boolean invoke(C newConfig) {
                    return oldConfig.getName().equals(newConfig.getName()) &&
                            oldConfig.getType().equals(newConfig.getType());
                }
            });

            if (!oldConfig.equals(newConfig)) {
                watcher.onModify(newConfig);
            }
        }
    }
}
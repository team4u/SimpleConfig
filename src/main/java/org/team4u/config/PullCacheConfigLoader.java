package org.team4u.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.team4u.kit.core.action.Function;
import org.team4u.kit.core.error.ExceptionUtil;
import org.team4u.kit.core.lang.LongTimeThread;
import org.team4u.kit.core.log.LogMessage;
import org.team4u.kit.core.util.CollectionExUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主动拉取缓冲配置加载器
 *
 * @author Jay.Wu
 */
public class PullCacheConfigLoader<C extends SystemConfig> extends AbstractConfigLoader<C> {

    private final Log log = LogFactory.get();

    /**
     * 代理配置集合缓存
     */
    private List<C> configCache;
    /**
     * 配置类代理映射
     */
    private Map<Class, Object> toTypeProxies = new HashMap<Class, Object>();

    private ConfigLoader<C> delegateConfigLoader;
    private int refreshIntervalMs;
    private Watcher<C> watcher;

    private RefreshWorker refreshWorker = new RefreshWorker();

    /**
     * @param delegateConfigLoader 代理配置加载器
     * @param refreshIntervalMs    缓存刷新间隔时间（毫秒）
     */
    public PullCacheConfigLoader(ConfigLoader<C> delegateConfigLoader, int refreshIntervalMs) {
        this(delegateConfigLoader, refreshIntervalMs, null);
    }

    /**
     * @param delegateConfigLoader 代理配置加载器
     * @param refreshIntervalMs    缓存刷新间隔时间（毫秒），0则不开启刷新
     * @param watcher              配置变动观察者
     */
    public PullCacheConfigLoader(ConfigLoader<C> delegateConfigLoader, int refreshIntervalMs, Watcher<C> watcher) {
        this.delegateConfigLoader = delegateConfigLoader;
        this.watcher = watcher;
        this.refreshIntervalMs = refreshIntervalMs;

        if (refreshIntervalMs > 0) {
            refreshWorker.start();
        }
    }

    @Override
    public List<C> load() {
        return delegateConfigLoader.load();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T to(Class<T> toType, String prefix) {
        LogMessage lm = new LogMessage(this.getClass().getSimpleName(), "to")
                .append("toType", toType.getSimpleName());
        // 从缓存读取配置对象
        if (toTypeProxies.containsKey(toType)) {
            log.debug(lm.success().append("mode", "cache").toString());
            return (T) toTypeProxies.get(toType);
        }

        synchronized (this) {
            if (!toTypeProxies.containsKey(toType)) {
                try {
                    // 创建配置对象并缓存
                    T proxy = delegateConfigLoader.to(toType, prefix);
                    toTypeProxies.put(toType, proxy);
                    log.info(lm.success().append("mode", "new").toString());
                    return proxy;
                } catch (Exception e) {
                    log.error(e, lm.fail().append("mode", "new").toString());
                    throw ExceptionUtil.toRuntimeException(e);
                }
            } else {
                log.debug(lm.success().append("mode", "cache").toString());
                return (T) toTypeProxies.get(toType);
            }
        }
    }

    @Override
    public void close() throws IOException {
        refreshWorker.close();
        delegateConfigLoader.close();
    }

    /**
     * 加载并比较更新配置
     */
    private void loadAndDiffConfigs() {
        try {
            List<C> oldConfigs = configCache;
            configCache = load();
            // 若最新配置存在变化，则更新缓存的配置对象字段值
            if (diffConfigs(oldConfigs, configCache)) {
                for (Map.Entry<Class, Object> entry : toTypeProxies.entrySet()) {
                    BeanUtil.copyProperties(delegateConfigLoader.to(entry.getKey()), entry.getValue());
                }
            }
        } catch (Throwable e) {
            watcher.onError(e);
        }
    }

    /**
     * 比较配置是否变化
     */
    private boolean diffConfigs(List<C> oldConfigs, List<C> newConfigs) {
        // 若无缓存配置则表示初次初始化，无需比较
        //noinspection SimplifiableIfStatement
        if (oldConfigs == null) {
            return true;
        }

        boolean hasCreatedConfigs = diffCreatedConfigs(oldConfigs, newConfigs);
        boolean hasDeletedConfigs = diffDeletedConfigs(oldConfigs, newConfigs);
        boolean hasModifyConfigs = diffModifyConfigs(oldConfigs, newConfigs);

        return hasCreatedConfigs || hasDeletedConfigs || hasModifyConfigs;
    }

    /**
     * 比较配置是否新增
     */
    private boolean diffCreatedConfigs(List<C> oldConfigs, List<C> newConfigs) {
        boolean hasCreatedConfig = false;

        for (final C newConfig : newConfigs) {
            if (!CollectionExUtil.any(oldConfigs, new Function<C, Boolean>() {
                @Override
                public Boolean invoke(C oldConfig) {
                    return oldConfig.getName().equals(newConfig.getName()) &&
                            oldConfig.getType().equals(newConfig.getType());
                }
            })) {
                hasCreatedConfig = true;

                log.info(new LogMessage(this.getClass().getSimpleName(), "diffCreatedConfigs")
                        .success()
                        .append("newConfig", newConfig)
                        .toString());

                if (watcher != null) {
                    watcher.onCreate(newConfig);
                }
            }
        }

        return hasCreatedConfig;
    }

    /**
     * 比较配置是否删除
     */
    private boolean diffDeletedConfigs(List<C> oldConfigs, List<C> newConfigs) {
        boolean hasDeletedConfig = false;

        for (final C oldConfig : oldConfigs) {
            if (!CollectionExUtil.any(newConfigs, new Function<C, Boolean>() {
                @Override
                public Boolean invoke(C newConfig) {
                    return oldConfig.getName().equals(newConfig.getName()) &&
                            oldConfig.getType().equals(newConfig.getType());
                }
            })) {
                hasDeletedConfig = true;

                log.info(new LogMessage(this.getClass().getSimpleName(), "diffDeletedConfigs")
                        .success()
                        .append("oldConfig", oldConfig)
                        .toString());

                if (watcher != null) {
                    watcher.onDelete(oldConfig);
                }
            }
        }

        return hasDeletedConfig;
    }

    /**
     * 比较配置是否修改
     */
    private boolean diffModifyConfigs(List<C> oldConfigs, List<C> newConfigs) {
        boolean hasModifiedConfig = false;

        for (C oldConfig : oldConfigs) {
            for (C newConfig : newConfigs) {
                if (oldConfig.equals(newConfig)) {
                    if (!StrUtil.equals(oldConfig.getValue(), newConfig.getValue()) ||
                            !StrUtil.equals(oldConfig.getDescription(), newConfig.getDescription()) ||
                            oldConfig.getSequenceNo() != newConfig.getSequenceNo() ||
                            oldConfig.getEnabled() != newConfig.getEnabled()) {
                        hasModifiedConfig = true;

                        log.info(new LogMessage(this.getClass().getSimpleName(), "diffModifyConfigs")
                                .success()
                                .append("oldConfig", oldConfig)
                                .append("newConfig", newConfig)
                                .toString());

                        if (watcher != null) {
                            watcher.onModify(newConfig);
                        }
                    }
                }
            }
        }

        return hasModifiedConfig;
    }

    /**
     * 缓存更新器
     */
    private class RefreshWorker extends LongTimeThread {

        @Override
        protected void onRun() {
            ThreadUtil.safeSleep(refreshIntervalMs);
            loadAndDiffConfigs();
        }
    }
}
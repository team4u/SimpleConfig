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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库缓冲配置加载器
 *
 * @author Jay.Wu
 */
public class DbCacheConfigLoader<C extends SystemConfig> implements ConfigLoader<C> {

    private final Log log = LogFactory.get();

    /**
     * 数据库配置集合缓存
     */
    private List<C> configCache;
    /**
     * 配置类代理映射
     */
    private Map<Class, Object> toTypeProxy = new HashMap<Class, Object>();

    private DbConfigLoader<C> dbConfigLoader;
    private int refreshIntervalMs;
    private Watcher<C> watcher;

    private RefreshWorker refreshWorker = new RefreshWorker();

    /**
     * @param dbConfigLoader    数据库配置加载器
     * @param refreshIntervalMs 缓存刷新间隔时间（毫秒）
     */
    public DbCacheConfigLoader(DbConfigLoader<C> dbConfigLoader, int refreshIntervalMs) {
        this(dbConfigLoader, refreshIntervalMs, null);
    }

    /**
     * @param dbConfigLoader    数据库配置加载器
     * @param refreshIntervalMs 缓存刷新间隔时间（毫秒）
     * @param watcher           配置变动观察者
     */
    public DbCacheConfigLoader(DbConfigLoader<C> dbConfigLoader, int refreshIntervalMs, Watcher<C> watcher) {
        this.dbConfigLoader = dbConfigLoader;
        this.watcher = watcher;
        this.refreshIntervalMs = refreshIntervalMs;

        if (refreshIntervalMs > 0) {
            refreshWorker.start();
        }
    }

    @Override
    public List<C> load() {
        return dbConfigLoader.load();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T to(Class<T> toType) {
        LogMessage lm = new LogMessage(this.getClass().getSimpleName(), "to")
                .append("toType", toType.getSimpleName());
        if (toTypeProxy.containsKey(toType)) {
            log.debug(lm.success().append("mode", "cache").toString());
            return (T) toTypeProxy.get(toType);
        }

        synchronized (this) {
            if (!toTypeProxy.containsKey(toType)) {
                try {
                    T proxy = dbConfigLoader.to(toType);
                    toTypeProxy.put(toType, proxy);
                    log.info(lm.success().append("mode", "new").toString());
                    return proxy;
                } catch (Exception e) {
                    log.error(e, lm.fail().append("mode", "new").toString());
                    throw ExceptionUtil.toRuntimeException(e);
                }
            } else {
                log.debug(lm.success().append("mode", "cache").toString());
                return (T) toTypeProxy.get(toType);
            }
        }
    }

    @Override
    public void close() {
        refreshWorker.close();
        dbConfigLoader.close();
    }

    private void loadAndDiffConfigs() {
        try {
            List<C> oldConfigs = configCache;
            configCache = load();
            if (diffConfigs(oldConfigs, configCache)) {
                for (Map.Entry<Class, Object> entry : toTypeProxy.entrySet()) {
                    BeanUtil.copyProperties(dbConfigLoader.to(entry.getKey()), entry.getValue());
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
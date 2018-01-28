package org.team4u.config;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatchers;
import org.team4u.aop.MethodInterceptor;
import org.team4u.aop.SimpleAop;
import org.team4u.kit.core.action.Function;
import org.team4u.kit.core.error.ExceptionUtil;
import org.team4u.kit.core.lang.LongTimeThread;
import org.team4u.kit.core.log.LogMessage;
import org.team4u.kit.core.util.CollectionExUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

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
     * 配置类代理
     */
    private Object configProxy;


    private DbConfigLoader<C> dbConfigLoader;
    private int refreshIntervalMs;
    private Watcher<C> watcher;

    private ConfigObjectInterceptor configObjectInterceptor = new ConfigObjectInterceptor();
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
        if (configProxy != null) {
            log.debug(lm.success().append("proxy", "found").toString());
            return (T) configProxy;
        }

        synchronized (this) {
            if (configProxy == null) {
                try {
                    configObjectInterceptor.setTarget(dbConfigLoader.to(toType));
                    configProxy = SimpleAop.createClass(
                            toType,
                            ElementMatchers.<MethodDescription>any(),
                            configObjectInterceptor).newInstance();
                    log.info(lm.success().append("configProxy", "created").toString());
                } catch (Exception e) {
                    log.error(e, lm.fail().append("configProxy", "created").toString());
                    ExceptionUtil.throwRuntimeExceptionOrError(e);
                }
            }
        }

        //noinspection unchecked
        return (T) configProxy;
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
                configObjectInterceptor.setTarget(
                        dbConfigLoader.to(configObjectInterceptor.target.getClass())
                );
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


    /**
     * 配置类拦截器
     */
    protected class ConfigObjectInterceptor implements MethodInterceptor {

        /**
         * 实际配置类
         */
        private Object target;

        void setTarget(Object target) {
            this.target = target;
        }

        @Override
        public Object intercept(Object instance, Object[] parameters,
                                Method method, Callable<?> superMethod) {
            return ReflectUtil.invoke(target, method.getName(), parameters);
        }
    }
}
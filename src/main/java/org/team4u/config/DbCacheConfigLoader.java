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
import org.team4u.kit.core.util.CollectionExUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Jay.Wu
 */
public class DbCacheConfigLoader<C extends SystemConfig> implements ConfigLoader<C> {

    private final Log log = LogFactory.get();

    private List<C> configs;
    private Object proxy;
    private ConfigObjectInterceptor configObjectInterceptor = new ConfigObjectInterceptor();
    private int cacheTimeoutMs;

    private Watcher<C> watcher;

    private DbConfigLoader<C> dbConfigLoader;

    private Worker worker = new Worker();

    public DbCacheConfigLoader(DbConfigLoader<C> dbConfigLoader,
                               int cacheTimeoutMs, Watcher<C> watcher) {
        this.dbConfigLoader = dbConfigLoader;
        this.watcher = watcher;
        this.cacheTimeoutMs = cacheTimeoutMs;

        if (cacheTimeoutMs > 0) {
            worker.start();
        }
    }

    @Override
    public List<C> load() {
        return dbConfigLoader.load();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T to(Class<T> toType) {
        if (proxy != null) {
            log.trace("loadAllEnabledConfigs from cache(class={})", toType.getSimpleName());
            return (T) proxy;
        }

        synchronized (this) {
            if (proxy == null) {
                try {
                    configObjectInterceptor.setConfigObject(dbConfigLoader.to(toType));
                    proxy = SimpleAop.createClass(toType,
                            ElementMatchers.<MethodDescription>any(),
                            configObjectInterceptor).newInstance();
                } catch (Exception e) {
                    ExceptionUtil.throwRuntimeExceptionOrError(e);
                }
            }
        }

        //noinspection unchecked
        return (T) proxy;
    }

    @Override
    public void close() {
        worker.close();
        dbConfigLoader.close();
    }

    protected void refreshAndCompareConfigs() {
        List<C> oldConfigs = configs;
        configs = load();
        if (compareConfigs(oldConfigs, configs)) {
            configObjectInterceptor.setConfigObject(
                    dbConfigLoader.to(configObjectInterceptor.configObject.getClass())
            );
        }
    }

    private boolean compareConfigs(List<C> oldConfigs, List<C> newConfigs) {
        if (oldConfigs == null) {
            return true;
        }

        return compareCreatedConfigs(oldConfigs, newConfigs) ||
                compareDeletedConfigs(oldConfigs, newConfigs) ||
                compareModifyConfigs(oldConfigs, newConfigs);

    }

    /**
     * 比较新增的配置项
     */
    private boolean compareCreatedConfigs(List<C> oldConfigs, List<C> newConfigs) {
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

                if (watcher != null) {
                    watcher.onCreate(newConfig);
                }
            }
        }

        return hasCreatedConfig;
    }

    /**
     * 比较删除的配置项
     */
    private boolean compareDeletedConfigs(List<C> oldConfigs, List<C> newConfigs) {
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

                if (watcher != null) {
                    watcher.onDelete(oldConfig);
                }
            }
        }

        return hasDeletedConfig;
    }

    /**
     * 比较修改的配置项
     */
    private boolean compareModifyConfigs(List<C> oldConfigs, List<C> newConfigs) {
        boolean hasModifiedConfig = false;


        for (C oldConfig : oldConfigs) {
            for (C newConfig : newConfigs) {
                if (oldConfig.equals(newConfig)) {
                    if (!StrUtil.equals(oldConfig.getValue(), newConfig.getValue()) ||
                            !StrUtil.equals(oldConfig.getDescription(), newConfig.getDescription()) ||
                            oldConfig.getSequenceNo() != newConfig.getSequenceNo() ||
                            oldConfig.getEnabled() != newConfig.getEnabled()) {
                        hasModifiedConfig = true;

                        if (watcher != null) {
                            watcher.onModify(newConfig);
                        }
                    }
                }
            }
        }

        return hasModifiedConfig;
    }

    private class Worker extends LongTimeThread {

        @Override
        protected void onRun() {
            ThreadUtil.safeSleep(cacheTimeoutMs);
            refreshAndCompareConfigs();
        }
    }


    protected class ConfigObjectInterceptor implements MethodInterceptor {

        private Object configObject;

        public ConfigObjectInterceptor setConfigObject(Object configObject) {
            this.configObject = configObject;
            return this;
        }

        @Override
        public Object intercept(Object instance, Object[] parameters,
                                Method method, Callable<?> superMethod) {
            return ReflectUtil.invoke(configObject, method.getName(), parameters);
        }
    }
}
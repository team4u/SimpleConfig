package org.team4u.config;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

/**
 * @author Jay.Wu
 */
public class DefaultWatcher<C extends SystemConfig> implements Watcher<C> {

    private Log log = LogFactory.get();

    @Override
    public void onCreate(SystemConfig newConfig) {

    }

    @Override
    public void onModify(SystemConfig newConfig) {

    }

    @Override
    public void onDelete(SystemConfig oldConfig) {

    }

    @Override
    public void onError(Throwable e) {
        log.error(e, e.getMessage());
    }
}
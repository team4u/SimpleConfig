package org.team4u.config;

/**
 * 配置变动观察者
 *
 * @author Jay.Wu
 */
public interface Watcher<C extends SystemConfig> {
    /**
     * 创建时执行的方法
     */
    void onCreate(C newConfig);

    /**
     * 修改时执行的方法
     */
    void onModify(C newConfig);

    /**
     * 删除时执行的方法
     */
    void onDelete(C oldConfig);

    /**
     * 出错时执行的方法
     */
    void onError(Throwable e);
}
package org.team4u.test;

import cn.hutool.core.thread.ThreadUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.team4u.config.DefaultSystemConfig;
import org.team4u.config.PullCacheConfigLoader;
import org.team4u.config.Watcher;
import org.team4u.config.db.DbConfigLoader;
import org.team4u.dao.core.SimpleDao;
import org.team4u.sql.builder.util.SqlBuilders;

/**
 * @author Jay.Wu
 */
public class DbConfigLoaderTest extends ConfigLoaderTest {

    private static SimpleDao dao;

    @BeforeClass
    public static void init() {
        dao = TestUtil.createAndInitDao();
        dao.insert(createConfigs(), null, true);
    }

    @Test
    public void to() {
        checkTo(newLoader());
    }

    @Test
    public void cacheTo() {
        PullCacheConfigLoader<DefaultSystemConfig> loader = new PullCacheConfigLoader<DefaultSystemConfig>(
                newLoader(), 500, new Watcher<DefaultSystemConfig>() {

            @Override
            public void onCreate(DefaultSystemConfig newConfig) {
                System.out.println("onCreate");
            }

            @Override
            public void onModify(DefaultSystemConfig newConfig) {
                System.out.println("onModify");
            }

            @Override
            public void onDelete(DefaultSystemConfig oldConfig) {
                System.out.println("onDelete");
            }

            @Override
            public void onError(Throwable e) {

            }
        });

        Config c = checkTo(loader);

        dao.insert(new DefaultSystemConfig()
                .setType("app")
                .setName("g")
                .setValue("g")
                .setEnabled(true), null, true);

        dao.execute(SqlBuilders.update(DefaultSystemConfig.class)
                .setValue("value", "2")
                .where("name", "=", "a")
                .and("type", "=", "app")
                .create());

        dao.execute(SqlBuilders.delete(DefaultSystemConfig.class)
                .where("name", "=", "b")
                .and("type", "=", "app")
                .create());

        ThreadUtil.safeSleep(1000);

        Assert.assertEquals("g", c.getG());
        Assert.assertEquals(Integer.valueOf(2), c.getA());
        Assert.assertNull(c.getB());
    }

    private DbConfigLoader<DefaultSystemConfig> newLoader() {
        return new DbConfigLoader<DefaultSystemConfig>(DefaultSystemConfig.class, dao.getDataSource());
    }
}
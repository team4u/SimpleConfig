package org.team4u.test;

import cn.hutool.core.thread.ThreadUtil;
import org.junit.Assert;
import org.junit.Test;
import org.team4u.config.DbCacheConfigLoader;
import org.team4u.config.DbConfigLoader;
import org.team4u.config.DefaultSystemConfig;
import org.team4u.config.Watcher;
import org.team4u.dao.core.SimpleDao;
import org.team4u.sql.builder.util.SqlBuilders;

/**
 * @author Jay.Wu
 */
public class DbConfigLoaderTest extends ConfigLoaderTest {

    private SimpleDao dao = TestUtil.createAndInitDao();

    @Test
    public void to() {
        checkTo(newLoader());
    }

    @Test
    public void cacheTo() {
        DbCacheConfigLoader<DefaultSystemConfig> loader = new DbCacheConfigLoader<DefaultSystemConfig>(
                newLoader(), 1000, new Watcher<DefaultSystemConfig>() {

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

        dao.execute(SqlBuilders.update(DefaultSystemConfig.class)
                .setValue("value", "2")
                .where("name", "=", "a")
                .and("type", "=", "app")
                .create());

        ThreadUtil.safeSleep(1500);

        Assert.assertEquals(Integer.valueOf(2), c.getA());
    }

    private DbConfigLoader<DefaultSystemConfig> newLoader() {
        dao.insert(createConfigs(), null, true);
        return new DbConfigLoader<DefaultSystemConfig>(DefaultSystemConfig.class, dao.getDataSource());
    }
}

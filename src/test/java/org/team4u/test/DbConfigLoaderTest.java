package org.team4u.test;

import org.junit.Test;
import org.team4u.config.DbConfigLoader;
import org.team4u.config.DefaultSystemConfig;
import org.team4u.dao.core.SimpleDao;

/**
 * @author Jay.Wu
 */
public class DbConfigLoaderTest extends ConfigLoaderTest {

    @Test
    public void to() {
        checkTo(newLoader());
    }

    private DbConfigLoader<DefaultSystemConfig> newLoader() {
        SimpleDao dao = TestUtil.createAndInitDao();
        dao.insert(createConfigs(), null, true);
        return new DbConfigLoader<DefaultSystemConfig>(DefaultSystemConfig.class, dao.getDataSource());
    }
}

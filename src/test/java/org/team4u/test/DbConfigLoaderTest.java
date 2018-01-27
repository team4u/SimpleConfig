package org.team4u.test;

import org.junit.Assert;
import org.junit.Test;
import org.team4u.config.ConfigLoader;
import org.team4u.config.DbConfigLoader;
import org.team4u.config.SystemConfig;
import org.team4u.dao.core.SimpleDao;

import java.util.List;

/**
 * @author Jay.Wu
 */
public class DbConfigLoaderTest extends ConfigLoaderTest {

    @Test
    public void load() {
        ConfigLoader loader = newLoader();
        List<SystemConfig> x = loader.load();
        Assert.assertEquals(6, x.size());
    }

    @Test
    public void to() {
        checkTo(newLoader());
    }

    private DbConfigLoader newLoader() {
        SimpleDao dao = TestUtil.createAndInitDao();
        dao.insert(createConfigs(), null, true);
        return new DbConfigLoader(dao.getDataSource());
    }
}

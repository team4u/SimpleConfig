package org.team4u.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.setting.dialect.Props;
import org.junit.Assert;
import org.junit.Test;
import org.team4u.config.DefaultSystemConfig;
import org.team4u.config.PullCacheConfigLoader;
import org.team4u.config.Watcher;
import org.team4u.config.props.PropsConfigLoader;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Jay.Wu
 */
public class PropsConfigLoaderTest extends ConfigLoaderTest {

    private final static String CONFIG_PATH = "config.properties";

    @Test
    public void to() {
        checkTo(newLoader());
    }

    @Test
    public void cacheTo() throws IOException {
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

        Properties oldProps = Props.getProp(CONFIG_PATH);
        Properties newProps = new Properties(oldProps);
        newProps.setProperty("app.g", "g");
        newProps.setProperty("app.a", "2");
        newProps.remove("app.b");

        newProps.store(FileUtil.getOutputStream(CONFIG_PATH), "");
        ThreadUtil.safeSleep(2000);

        Assert.assertEquals("g", c.getG());
        Assert.assertEquals(Integer.valueOf(2), c.getA());
        Assert.assertNull(c.getB());

        oldProps.store(FileUtil.getOutputStream(CONFIG_PATH), "");
    }

    private PropsConfigLoader newLoader() {
        return new PropsConfigLoader(CONFIG_PATH);
    }
}

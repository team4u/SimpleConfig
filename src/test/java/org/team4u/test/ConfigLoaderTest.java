package org.team4u.test;

import cn.hutool.core.collection.CollUtil;
import org.junit.Assert;
import org.junit.Test;
import org.team4u.config.AbstractConfigLoader;
import org.team4u.config.ConfigLoader;
import org.team4u.config.ConfigurationProperties;
import org.team4u.config.SystemConfig;

import java.util.List;

/**
 * @author Jay.Wu
 */
public class ConfigLoaderTest {

    @Test
    public void to() {
        ConfigLoader loader = new AbstractConfigLoader() {

            @Override
            public List<SystemConfig> load() {
                return createConfigs();
            }
        };

        checkTo(loader);
    }

    protected List<SystemConfig> createConfigs() {
        return CollUtil.newArrayList(
                new SystemConfig().setType("app").setName("a").setValue("1").setEnabled(true),
                new SystemConfig().setType("app").setName("b").setValue("0").setEnabled(true),
                new SystemConfig().setType("app").setName("c").setValue("1").setEnabled(true),
                new SystemConfig().setType("app").setName("d").setValue("1").setEnabled(true),
                new SystemConfig().setType("app").setName("d").setValue("2").setEnabled(true),
                new SystemConfig().setType("app").setName("e").setValue("{'name':'fjay','age':1}").setEnabled(true)
        );
    }

    protected void checkTo(ConfigLoader loader) {
        Config config = loader.to(Config.class);
        Assert.assertEquals(Integer.valueOf(1), config.a);
        Assert.assertEquals(Boolean.FALSE, config.b);
        Assert.assertEquals(Boolean.TRUE, config.c);
        Assert.assertArrayEquals(CollUtil.newArrayList(1, 2).toArray(), config.d);
        Assert.assertEquals("fjay", config.e.name);
        Assert.assertEquals(Integer.valueOf(1), config.e.age);
    }

    @ConfigurationProperties("app")
    public static class Config {

        Integer a;

        Boolean b;

        Boolean c;

        Integer[] d;

        E e;

        public static class E {

            String name;

            Integer age;

            public String getName() {
                return name;
            }

            public E setName(String name) {
                this.name = name;
                return this;
            }

            public Integer getAge() {
                return age;
            }

            public E setAge(Integer age) {
                this.age = age;
                return this;
            }
        }
    }
}
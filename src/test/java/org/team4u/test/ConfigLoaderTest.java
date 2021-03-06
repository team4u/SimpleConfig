package org.team4u.test;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.Test;
import org.team4u.config.*;

import java.util.List;

/**
 * @author Jay.Wu
 */
public class ConfigLoaderTest {

    @Test
    public void to() {
        ConfigLoader<DefaultSystemConfig> loader = new AbstractConfigLoader<DefaultSystemConfig>() {

            @Override
            public void close() {

            }

            @Override
            public List<DefaultSystemConfig> load() {
                return createConfigs();
            }
        };

        checkTo(loader);
    }

    protected static List<DefaultSystemConfig> createConfigs() {
        return CollUtil.newArrayList(
                new DefaultSystemConfig().setType("app").setName("a").setValue("1").setEnabled(true),
                new DefaultSystemConfig().setType("app").setName("b").setValue("0").setEnabled(true),
                new DefaultSystemConfig().setType("app").setName("c").setValue("1").setEnabled(true),
                new DefaultSystemConfig().setType("app").setName("d").setValue("2,1").setEnabled(true),
                new DefaultSystemConfig().setType("app").setName("e").setValue("{'name':'fjay','age':1}").setEnabled(true),
                new DefaultSystemConfig().setType("app").setName("f").setValue("f").setEnabled(false),
                new DefaultSystemConfig().setType("app").setName("h").setValue("2, 1").setEnabled(true),
                new DefaultSystemConfig().setType("app").setName("j").setValue("j").setEnabled(true)
        );
    }

    protected Config checkTo(ConfigLoader<DefaultSystemConfig> loader) {
        Config c1 = checkTo(loader, false);
        Config c2 = checkTo(loader, true);

        Assert.assertEquals(JSON.toJSON(c1), JSON.toJSON(c2));
        return c1;
    }

    protected Config checkTo(ConfigLoader<DefaultSystemConfig> loader, boolean withPrefix) {
        Config config;

        if (withPrefix) {
            config = loader.to(Config.class, "app");
        } else {
            config = loader.to(Config.class);
        }
        Assert.assertEquals(Integer.valueOf(1), config.getA());
        Assert.assertEquals(Boolean.FALSE, config.getB());
        Assert.assertEquals(Boolean.TRUE, config.getC());
        Assert.assertArrayEquals(CollUtil.newArrayList(2, 1).toArray(), config.getD());
        Assert.assertEquals("fjay", config.getE().getName());
        Assert.assertEquals(Integer.valueOf(1), config.getE().getAge());
        Assert.assertNull(config.getF());
        Assert.assertEquals(CollUtil.newArrayList(2, 1), config.getH());
        Assert.assertEquals("i", config.getI());
        Assert.assertNull(config.getJ());

        return config;
    }

    @ConfigurationProperties("app")
    public static class Config {

        Integer a;

        Boolean b;

        Boolean c;

        Integer[] d;

        E e;

        String f;

        String g;

        List<Integer> h;

        String i = "i";

        @IgnoreField
        String j;

        public Integer getA() {
            return a;
        }

        public Config setA(Integer a) {
            this.a = a;
            return this;
        }

        public Boolean getB() {
            return b;
        }

        public Config setB(Boolean b) {
            this.b = b;
            return this;
        }

        public Boolean getC() {
            return c;
        }

        public Config setC(Boolean c) {
            this.c = c;
            return this;
        }

        public Integer[] getD() {
            return d;
        }

        public Config setD(Integer[] d) {
            this.d = d;
            return this;
        }

        public E getE() {
            return e;
        }

        public Config setE(E e) {
            this.e = e;
            return this;
        }

        public String getF() {
            return f;
        }

        public Config setF(String f) {
            this.f = f;
            return this;
        }

        public String getG() {
            return g;
        }

        public Config setG(String g) {
            this.g = g;
            return this;
        }

        public List<Integer> getH() {
            return h;
        }

        public Config setH(List<Integer> h) {
            this.h = h;
            return this;
        }

        public String getI() {
            return i;
        }

        public Config setI(String i) {
            this.i = i;
            return this;
        }

        public String getJ() {
            return j;
        }

        public Config setJ(String j) {
            this.j = j;
            return this;
        }

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
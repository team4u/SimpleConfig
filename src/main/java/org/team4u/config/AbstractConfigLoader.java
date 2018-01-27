package org.team4u.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import org.team4u.kit.core.util.CollectionExUtil;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;


/**
 * @author Jay.Wu
 */
public abstract class AbstractConfigLoader<C extends SystemConfig> implements ConfigLoader<C> {

    private final Log log = LogFactory.get();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T to(Class<T> toType) {
        final ConfigurationProperties cp = toType.getAnnotation(ConfigurationProperties.class);
        Assert.notNull(cp, "请添加@ConfigurationProperties注解");

        Collection<? extends SystemConfig> configs = CollUtil.filter(load(), new Filter() {
            @Override
            public boolean accept(Object o) {
                SystemConfig systemConfig = (SystemConfig) o;
                return systemConfig.getEnabled() &&
                        StrUtil.equalsIgnoreCase(systemConfig.getType(), cp.value());
            }
        });

        T toConfigObject = ReflectUtil.newInstance(toType);

        for (final Field field : ReflectUtil.getFields(toType)) {
            Collection<? extends SystemConfig> configsForField = CollUtil.filter(configs, new Filter() {
                @Override
                public boolean accept(Object o) {
                    SystemConfig systemConfig = (SystemConfig) o;
                    return StrUtil.equalsIgnoreCase(field.getName(), systemConfig.getName());
                }
            });

            if (configsForField.isEmpty()) {
                continue;
            }

            if (ClassUtil.isSimpleValueType(field.getType())) {
                SystemConfig config = mustUnique(configsForField);
                ReflectUtil.setFieldValue(toConfigObject, field,
                        Convert.convert(field.getType(), config.getValue()));
            } else if (field.getType().isArray()) {
                List<String> values = CollectionExUtil.collectWithKey(configsForField, "value");

                ReflectUtil.setFieldValue(toConfigObject, field,
                        Convert.convert(field.getType(), values.toArray()));
            } else {
                SystemConfig config = CollUtil.getFirst(configsForField);
                Assert.isFalse(Collection.class.isAssignableFrom(field.getType()),
                        "配置属性不允许为Collection，请使用Array|name={}|type={}",
                        config.getName(), config.getType());
                mustUnique(configsForField);
                Object value = JSON.parseObject(config.getValue(), field.getType());
                ReflectUtil.setFieldValue(toConfigObject, field, value);
            }
        }

        return toConfigObject;
    }

    private SystemConfig mustUnique(Collection<? extends SystemConfig> configs) {
        SystemConfig config = CollUtil.getFirst(configs);

        Assert.isTrue(configs.size() == 1,
                "配置名称不唯一|name={}|type={}", config.getName(), config.getType());

        return config;
    }
}
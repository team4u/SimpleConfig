package org.team4u.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import org.team4u.kit.core.util.FieldUtil;

import java.lang.reflect.Field;
import java.util.Collection;


/**
 * 抽象配置加载器
 *
 * @author Jay.Wu
 */
public abstract class AbstractConfigLoader<C extends SystemConfig> implements ConfigLoader<C> {

    public <T> T to(Class<T> toType) {
        final ConfigurationProperties cp = toType.getAnnotation(ConfigurationProperties.class);

        String prefix = null;
        if (cp != null) {
            prefix = cp.value();
        }
        return to(toType, prefix);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T to(Class<T> toType, final String prefix) {
        // 获取开启的且类型符合的配置集合
        Collection<? extends SystemConfig> configs = CollUtil.filter(load(), new Filter() {
            @Override
            public boolean accept(Object o) {
                SystemConfig systemConfig = (SystemConfig) o;
                return systemConfig.getEnabled() &&
                        StrUtil.equalsIgnoreCase(systemConfig.getType(), prefix);
            }
        });

        T toConfigObject = ReflectUtil.newInstance(toType);

        for (final Field field : ReflectUtil.getFields(toType)) {
            // 获取满足该字段的所有配置集合
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

            // 确保只有一个满足
            SystemConfig config = mustUnique(configsForField);

            // 简单类型直接注入
            if (ClassUtil.isSimpleTypeOrArray(field.getType())) {
                ReflectUtil.setFieldValue(toConfigObject, field,
                        Convert.convert(field.getType(), config.getValue()));
            } else if (Collection.class.isAssignableFrom(field.getType())) {
                // 集合类型需要获取泛型类型，目前只支持一级泛型
                ReflectUtil.setFieldValue(toConfigObject, field,
                        Convert.toCollection(
                                field.getType(),
                                FieldUtil.getGenericTypes(field, 0),
                                StrUtil.splitTrim(config.getValue(), ","))
                );
            } else {
                // 复杂类型只支持json格式
                Object value = JSON.parseObject(config.getValue(), field.getType());
                ReflectUtil.setFieldValue(toConfigObject, field, value);
            }
        }

        return toConfigObject;
    }

    /**
     * 确保唯一性
     */
    private SystemConfig mustUnique(Collection<? extends SystemConfig> configs) {
        SystemConfig config = CollUtil.getFirst(configs);

        Assert.isTrue(configs.size() == 1,
                "配置名称不唯一|name={}|type={}", config.getName(), config.getType());

        return config;
    }
}
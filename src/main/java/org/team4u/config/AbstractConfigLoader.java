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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;


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

            SystemConfig config = mustUnique(configsForField);

            if (ClassUtil.isSimpleTypeOrArray(field.getType())) {
                ReflectUtil.setFieldValue(toConfigObject, field,
                        Convert.convert(field.getType(), config.getValue()));
            } else if (Collection.class.isAssignableFrom(field.getType())) {
                ReflectUtil.setFieldValue(toConfigObject, field,
                        Convert.toCollection(field.getType(), getCollActualType(field),
                                StrUtil.splitTrim(config.getValue(), ",")));
            } else {
                mustUnique(configsForField);
                Object value = JSON.parseObject(config.getValue(), field.getType());
                ReflectUtil.setFieldValue(toConfigObject, field, value);
            }
        }

        return toConfigObject;
    }

    /**
     * 获取集合属性声明的第一级泛型
     */
    private Class getCollActualType(Field field) {
        Type genericFieldType = field.getGenericType();

        if (genericFieldType instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) genericFieldType;
            Type[] fieldArgTypes = aType.getActualTypeArguments();
            return CollectionExUtil.getFirst(fieldArgTypes);
        }

        return null;
    }

    private SystemConfig mustUnique(Collection<? extends SystemConfig> configs) {
        SystemConfig config = CollUtil.getFirst(configs);

        Assert.isTrue(configs.size() == 1,
                "配置名称不唯一|name={}|type={}", config.getName(), config.getType());

        return config;
    }
}
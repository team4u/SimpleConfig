package org.team4u.config.props;

import cn.hutool.core.io.FileUtil;
import org.team4u.config.AbstractConfigLoader;
import org.team4u.config.DefaultSystemConfig;
import org.team4u.kit.core.action.Function;
import org.team4u.kit.core.error.ExceptionUtil;
import org.team4u.kit.core.util.CollectionExUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 配置文件配置加载器
 *
 * @author Jay.Wu
 */
public class PropsConfigLoader extends AbstractConfigLoader<DefaultSystemConfig> {

    private String path;

    public PropsConfigLoader(String path) {
        this.path = path;
    }

    @Override
    public List<DefaultSystemConfig> load() {
        try {
            Properties props = new Properties();
            props.load(FileUtil.getInputStream(path));

            return CollectionExUtil.collect(props.entrySet(),
                    new Function<Map.Entry<Object, Object>, DefaultSystemConfig>() {
                        @Override
                        public DefaultSystemConfig invoke(Map.Entry<Object, Object> obj) {
                            String typeAndName = obj.getKey().toString();
                            int index = typeAndName.lastIndexOf(".");
                            return new DefaultSystemConfig()
                                    .setEnabled(true)
                                    .setType(typeAndName.substring(0, index))
                                    .setName(typeAndName.substring(index + 1, typeAndName.length()))
                                    .setValue(obj.getValue().toString());
                        }
                    });
        } catch (IOException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    @Override
    public void close() {

    }
}
package org.team4u.config.props;

import cn.hutool.core.io.FileUtil;
import org.team4u.config.AbstractConfigLoader;
import org.team4u.config.DefaultSystemConfig;
import org.team4u.kit.core.action.Function;
import org.team4u.kit.core.error.ExceptionUtil;
import org.team4u.kit.core.util.CollectionExUtil;

import java.io.File;
import java.io.IOException;
import java.util.Date;
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

    /**
     * @param path 配置文件路径
     */
    public PropsConfigLoader(String path) {
        this.path = path;
    }

    @Override
    public List<DefaultSystemConfig> load() {
        try {
            final File propsFile = FileUtil.file(path);
            Properties props = new Properties();
            props.load(FileUtil.getInputStream(propsFile));

            final Date updateTime = new Date(propsFile.lastModified());

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
                                    .setValue(obj.getValue().toString())
                                    .setCreateTime(updateTime)
                                    .setUpdateTime(updateTime);
                        }
                    });
        } catch (IOException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    @Override
    public void close() {
        // Ignore
    }
}
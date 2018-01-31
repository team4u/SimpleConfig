package org.team4u.config.db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.team4u.config.AbstractConfigLoader;
import org.team4u.config.SystemConfig;
import org.team4u.dao.core.Dao;
import org.team4u.dao.core.SimpleDao;
import org.team4u.kit.core.log.LogMessage;
import org.team4u.sql.builder.util.SqlBuilders;

import javax.sql.DataSource;
import java.util.Comparator;
import java.util.List;

/**
 * @author Jay.Wu
 */
public class DbConfigLoader<C extends SystemConfig> extends AbstractConfigLoader<C> {

    private final Log log = LogFactory.get();

    private Dao dao;

    private Class<C> configType;

    public DbConfigLoader(Class<C> configType, DataSource dataSource) {
        this.configType = configType;
        dao = new SimpleDao(dataSource);
    }

    @Override
    public List<C> load() {
        List<C> result = dao.queryForList(SqlBuilders.select(configType));

        CollUtil.sort(result, new Comparator<SystemConfig>() {
            @Override
            public int compare(SystemConfig o1, SystemConfig o2) {
                return o1.getSequenceNo() - o2.getSequenceNo();
            }
        });

        if (log.isTraceEnabled()) {
            log.trace(new LogMessage(this.getClass().getSimpleName(), "load")
                    .success()
                    .append("size", result.size())
                    .toString());
        }
        return result;
    }

    @Override
    public void close() {
        // Ignore
    }
}
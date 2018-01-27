package org.team4u.config;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.team4u.dao.core.Dao;
import org.team4u.dao.core.SimpleDao;
import org.team4u.kit.core.log.LogMessage;
import org.team4u.sql.builder.util.SqlBuilders;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author Jay.Wu
 */
public class DbConfigLoader extends AbstractConfigLoader {

    private final Log log = LogFactory.get();

    private Dao dao;

    public DbConfigLoader(DataSource dataSource) {
        dao = new SimpleDao(dataSource);
    }

    @Override
    public List<SystemConfig> load() {
        List<SystemConfig> result = dao.queryForList(SqlBuilders.select(SystemConfig.class));

        if (log.isTraceEnabled()) {
            log.trace(new LogMessage(this.getClass().getSimpleName(), "load")
                    .success()
                    .append("size", result.size())
                    .toString());
        }
        return result;
    }
}
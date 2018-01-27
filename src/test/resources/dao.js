var ioc = {
    dataSource: {
        type: "com.alibaba.druid.pool.DruidDataSource",
        events: {
            depose: "close"
        },
        fields: {
            url: 'jdbc:mysql://127.0.0.1:3306/task_center?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&autoReconnectForPools=true',
            username: 'root',
            password: '1234',
            initialSize: 1,
            maxActive: 10,
            minIdle: 1,
            defaultAutoCommit: true,
            testWhileIdle: true,
            testOnBorrow: true,
            validationQuery: "select 1"
        }
    }
};

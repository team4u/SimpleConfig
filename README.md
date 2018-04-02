# SimpleConfig
[TOC]

SimpleConfig 是一个轻量级的配置加载器，适用于简单的配置场景。

## System Requirements

* JDK 1.6+

## Features

* 轻量级 - 整体代码结构简单,不同于功能强大的分布式配置中心，SimpleConfig支持最常用的数据库和配置文件，无需额外依赖
* 配置类 - 可以快速将数据库和配置文件内的属性映射为Java对象，并自动转换为类内字段类型
* 热更新 - 配置内容改变时，可自动更新Java对象，并支持内容监听

## Maven

```xml
<dependency>
    <groupId>org.team4u.config</groupId>
    <artifactId>simple-config</artifactId>
    <version>1.0.2</version>
</dependency>
<dependency>
    <groupId>org.team4u.dao</groupId>
    <artifactId>simple-dao</artifactId>
    <version>1.0.4</version>
</dependency>
<dependency>
    <groupId>org.team4u</groupId>
    <artifactId>team-kit-core</artifactId>
    <version>1.0.5</version>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-core</artifactId>
    <version>4.0.9</version>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-log</artifactId>
    <version>4.0.5</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.45</version>
</dependency>
```

添加仓库：

```xml
<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>bintray-team4u</id>
        <name>bintray</name>
        <url>https://dl.bintray.com/team4u/team4u</url>
    </repository>
</repositories>
```

## How To Use

### 数据库配置加载器

#### 创建配置表

```sql
CREATE TABLE system_config
(
  id          BIGINT UNSIGNED AUTO_INCREMENT
  COMMENT '自增长标识',
  is_enabled  TINYINT DEFAULT 1                                                   NOT NULL
  COMMENT '是否开启',
  type        VARCHAR(32) DEFAULT ''                                              NOT NULL
  COMMENT '类型',
  name        VARCHAR(255) DEFAULT ''                                             NOT NULL
  COMMENT '名称',
  value       VARCHAR(4000) DEFAULT ''                                            NOT NULL
  COMMENT '值',
  sequence_no BIGINT DEFAULT 0                                                    NOT NULL
  COMMENT '顺序号',
  description VARCHAR(255) DEFAULT ''                                             NOT NULL
  COMMENT '描述',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP                                 NOT NULL
  COMMENT '创建时间',
  update_time TIMESTAMP ON UPDATE CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP     NOT NULL
  COMMENT '更新时间',
  PRIMARY KEY (id)
)
  COMMENT '系统配置';

CREATE INDEX idx_type_name
  ON system_config (type, name);
```

插入数据

| 类型   | 名称   | 值                       | 是否开启 | 序号   | 描述   |
| ---- | ---- | ----------------------- | ---- | ---- | ---- |
| app  | a    | 1                       | 1    | 0    |      |
| app  | b    | 1                       | 1    | 0    |      |
| app  | d    | 1,2,3                   | 1    | 0    |      |
| app  | e    | {'name':'fjay','age':1} | 1    | 0    |      |
| app  | h    | 1,2,3                   | 1    | 0    |      |


#### 定义配置类

```java
@ConfigurationProperties("app")
public class Config {

    private Integer a;

    private Boolean b;

    private Integer[] d;

    private E e;
  
    private List<Integer> h;
    
    // 省略get/set方法
}

public class E {
  String name;
  Integer age;

  // 省略get/set方法
}
```

#### 使用加载器

```java
DbConfigLoader<DefaultSystemConfig> dbConfigLoader = new DbConfigLoader<DefaultSystemConfig>(DefaultSystemConfig.class, dataSource);

Config config = dbConfigLoader.to(Config.class);
```

### 配置文件加载器

#### 定义配置

```properties
app.a=1
app.b=1
app.d=2,1
app.e={'name':'fjay','age':1}
app.h=2, 1
```

#### 使用加载器

```java
PropsConfigLoader propsConfigLoader = new PropsConfigLoader("config.properties")

Config config = propsConfigLoader.to(Config.class);
```

### 缓存配置加载器

使用PullCacheConfigLoader可以将PropsConfigLoader或者DbConfigLoader生成的配置对象进行缓存，多次调用仅初始化一次配置类。

```java
PullCacheConfigLoader<DefaultSystemConfig> loader = new PullCacheConfigLoader<DefaultSystemConfig>(dbConfigLoader, 0);
Config config1 = loader.to(Config.class);
Config config2 = loader.to(Config.class);
// config1 == config2
```

PullCacheConfigLoader同时支持热更新，配置同步间隔时间即可。

```java
// 10s拉取一次数据库配置
PullCacheConfigLoader<DefaultSystemConfig> loader = new PullCacheConfigLoader<DefaultSystemConfig>(dbConfigLoader, 10000);
Config config = loader.to(Config.class);

// a的值为1
config.getA()
  
// 手工更改数据库a配置值为2
  
// 10s后a的值变为2
config.getA()
```
Watcher可以监听配置变化

```java
PullCacheConfigLoader<DefaultSystemConfig> loader = new PullCacheConfigLoader<DefaultSystemConfig>(
        dbConfigLoader, 60000, new Watcher<DefaultSystemConfig>() {

    @Override
    public void onCreate(DefaultSystemConfig newConfig) {
        System.out.println("onCreate");
    }

    @Override
    public void onModify(DefaultSystemConfig newConfig) {
        System.out.println("onModify");
    }

    @Override
    public void onDelete(DefaultSystemConfig oldConfig) {
        System.out.println("onDelete");
    }

    @Override
    public void onError(Throwable e) {
	   System.out.println("onError");
    }
});
```
END

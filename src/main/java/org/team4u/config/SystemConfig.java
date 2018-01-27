package org.team4u.config;

import org.team4u.sql.builder.entity.annotation.Column;
import org.team4u.sql.builder.entity.annotation.Id;
import org.team4u.sql.builder.entity.annotation.Table;

import java.util.Date;

/**
 * @author Jay.Wu
 */
@Table
public class SystemConfig {

    @Id(auto = true)
    private String id;

    @Column
    private String name;

    @Column
    private String type;

    @Column
    private String value;

    @Column
    private String description;

    @Column
    private int sequenceNo;

    @Column(name = "is_enabled")
    private Boolean enabled;

    @Column
    private Date createTime;

    @Column
    private Date updateTime;

    public String getId() {
        return id;
    }

    public SystemConfig setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public SystemConfig setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public SystemConfig setValue(String value) {
        this.value = value;
        return this;
    }

    public String getType() {
        return type;
    }

    public SystemConfig setType(String type) {
        this.type = type;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SystemConfig setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public SystemConfig setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public SystemConfig setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public SystemConfig setCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public SystemConfig setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }
}
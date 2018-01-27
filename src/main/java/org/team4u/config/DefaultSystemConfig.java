package org.team4u.config;

import org.team4u.sql.builder.entity.annotation.Column;
import org.team4u.sql.builder.entity.annotation.Id;
import org.team4u.sql.builder.entity.annotation.Table;

import java.util.Date;

/**
 * @author Jay.Wu
 */
@Table(name = "system_config")
public class DefaultSystemConfig implements SystemConfig<DefaultSystemConfig> {

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

    public DefaultSystemConfig setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DefaultSystemConfig setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public DefaultSystemConfig setValue(String value) {
        this.value = value;
        return this;
    }

    public String getType() {
        return type;
    }

    public DefaultSystemConfig setType(String type) {
        this.type = type;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public DefaultSystemConfig setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public DefaultSystemConfig setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public DefaultSystemConfig setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public DefaultSystemConfig setCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public DefaultSystemConfig setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultSystemConfig that = (DefaultSystemConfig) o;

        if (sequenceNo != that.sequenceNo) return false;
        if (!id.equals(that.id)) return false;
        if (!name.equals(that.name)) return false;
        if (!type.equals(that.type)) return false;
        if (!value.equals(that.value)) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return enabled.equals(that.enabled);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + sequenceNo;
        result = 31 * result + enabled.hashCode();
        return result;
    }
}
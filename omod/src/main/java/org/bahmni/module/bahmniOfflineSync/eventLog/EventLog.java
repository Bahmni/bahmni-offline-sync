package org.bahmni.module.bahmniOfflineSync.eventLog;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(
        name = "event_log"
)
public class EventLog {

    @Column(
            name = "category"
    )
    private String category;
    @Column(
            name = "id"
    )
    private Integer id;
    @Column(
            name = "uuid"
    )
    private String uuid;

    @Column(
            name = "timestamp"
    )
    private Date timestamp;

    @Column(
            name = "object"
    )
    private String object;

    @Column(
            name = "parent_uuid"
    )
    private String parentUuid;

    @Column(
            name = "filter"
    )
    private String filter;

    public EventLog(String uuid, String category, Date timestamp, String object, String filter, String parentUuid) {
        this.uuid = uuid;
        this.category = category;
        this.timestamp = timestamp;
        this.object = object;
        this.parentUuid = parentUuid;
        this.filter = filter;
    }

    public EventLog() {
    }


    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getFilter() {
        return filter;
    }

    public String getObject() {
        return object;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public String getParentUuid() {
        return parentUuid;
    }
}

package com.securegate.iam.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "system_settings")
public class SystemSetting implements Serializable {

    @Id
    private String settingKey;

    @Column(nullable = false)
    private String settingValue;

    private String description;

    private String category;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "is_editable")
    private boolean editable;

    @Column(name = "last_modified_at")
    private String lastModifiedAt;

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public String getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(String lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}

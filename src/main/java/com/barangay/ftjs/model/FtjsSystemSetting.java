package com.barangay.ftjs.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ftjs_system_settings")
@Getter
@Setter
@NoArgsConstructor
/**
 * JPA Entity storing global FTJS retention settings and system configurations.
 */
public class FtjsSystemSetting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 255)
    private String settingValue;

    public FtjsSystemSetting(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }
}

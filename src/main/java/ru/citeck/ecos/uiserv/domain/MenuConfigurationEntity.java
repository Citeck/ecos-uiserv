package ru.citeck.ecos.uiserv.domain;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "menu_configuration")
@Getter
@Setter
public class MenuConfigurationEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    @Column(name = "ext_id", nullable = false)
    private String extId;
    @NotNull
    @Column(name = "type", nullable = false)
    private String type;
    @Column
    private String authorities;
    @Column
    private String config;
    @Column(name = "model_version")
    private Integer modelVersion = 0;
    @Column
    private String localization;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MenuConfigurationEntity menuConfig = (MenuConfigurationEntity) o;
        if (menuConfig.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), menuConfig.getId());
    }
}

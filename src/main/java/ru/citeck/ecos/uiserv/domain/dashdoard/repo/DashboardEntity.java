package ru.citeck.ecos.uiserv.domain.dashdoard.repo;

import org.hibernate.annotations.ColumnTransformer;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "dashboards")
public class DashboardEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    private Long id;

    private String extId;
    private String name;
    private String typeRef;
    @ColumnTransformer(write = "LOWER(?)")
    private String authority;
    private String appliedToRef;
    @NotNull
    private String scope = "";
    private float priority;
    private byte[] config;

    private String workspace;

    public Long getId() {
        return id;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeRef() {
        return typeRef;
    }

    public void setTypeRef(String typeRef) {
        this.typeRef = typeRef;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getAppliedToRef() {
        return appliedToRef;
    }

    public void setAppliedToRef(String appliedToRef) {
        this.appliedToRef = appliedToRef;
    }

    @NotNull
    public String getScope() {
        return scope;
    }

    public void setScope(@NotNull String scope) {
        this.scope = scope;
    }

    public float getPriority() {
        return priority;
    }

    public void setPriority(float priority) {
        this.priority = priority;
    }

    public byte[] getConfig() {
        return config;
    }

    public void setConfig(byte[] config) {
        this.config = config;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DashboardEntity that = (DashboardEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

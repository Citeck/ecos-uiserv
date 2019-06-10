package ru.citeck.ecos.uiserv.domain;


import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * A File.
 */
@Entity
@Table(name = "files")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class File implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "file_id", nullable = false)
    private String fileId;

    /* This points to "active" (currently used) version of menu; however "active" does
        not necessarily mean "last" (with greatest ordinal number), there might exist some
        "standard" versions installed after latest "override" version, and in that case
        those "standard" version are not becoming "active" until the custom override is reverted. */
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    //@JoinColumn(name = "menu_config_version_id")
    private FileVersion fileVersion;

    @Column
    private Long latestOrdinal;

    @Column
    @Enumerated(EnumType.STRING)
    @Getter
    @Setter
    private FileType type;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public File fileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public FileVersion getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(FileVersion fileVersion) {
        this.fileVersion = fileVersion;
    }

    public Long getLatestOrdinal() {
        return latestOrdinal;
    }

    public void setLatestOrdinal(Long latestOrdinal) {
        this.latestOrdinal = latestOrdinal;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        File menuConfig = (File) o;
        if (menuConfig.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), menuConfig.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "File{" +
            "id=" + getId() +
            ", fileId='" + getFileId() + "'" +
            "}";
    }
}

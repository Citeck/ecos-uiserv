package ru.citeck.ecos.uiserv.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * A MenuConfigVersion.
 */
@Entity
@Table(name = "file_version")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FileVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /* This may contain null. For standard updates, this indicates that the menu is no longer
        considered existing since that version becomes active, like is't not in File table.
        For custom overrides, this means the same thing, but of course custom override
        can be reverted to latest standard version. */
    @Lob
    @Column(name = "bytes")
    private byte[] bytes;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(unique = true)
    private Translated translated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    @JsonIgnore
    @Getter
    @Setter
    private File file;

    @Column(nullable = false)
    private Long ordinal;

    @Column
    private Long productVersion;

    @Column(nullable = false)
    private Boolean isRevert;

    @Column
    @Getter
    @Setter
    private String contentType;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public Long getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(Long ordinal) {
        this.ordinal = ordinal;
    }

    public Long getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(Long productVersion) {
        this.productVersion = productVersion;
    }

    public Boolean getRevert() {
        return isRevert;
    }

    public void setRevert(Boolean revert) {
        isRevert = revert;
    }

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Translated getTranslated() {
        return translated;
    }

    public FileVersion translated(Translated translated) {
        this.translated = translated;
        return this;
    }

    public void setTranslated(Translated translated) {
        this.translated = translated;
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
        FileVersion fileVersion = (FileVersion) o;
        if (fileVersion.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), fileVersion.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "FileVersion{" +
            "id=" + getId() +
            ", contentType=" + getContentType() +
            "}";
    }
}

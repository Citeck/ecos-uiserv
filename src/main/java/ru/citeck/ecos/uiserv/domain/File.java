package ru.citeck.ecos.uiserv.domain;


import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;
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
    @Getter @Setter
    private Long id;

    @NotNull
    @Column(name = "file_id", nullable = false)
    @Getter @Setter
    private String fileId;

    /* This points to "active" (currently used) version of menu; however "active" does
        not necessarily mean "last" (with greatest ordinal number), there might exist some
        "standard" versions installed after latest "override" version, and in that case
        those "standard" version are not becoming "active" until the custom override is reverted. */
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    //@JoinColumn(name = "menu_config_version_id")
    @Getter @Setter
    private FileVersion fileVersion;

    @Column
    @Getter @Setter
    private Long latestOrdinal;

    @Column
    @Enumerated(EnumType.STRING)
    @Getter @Setter
    private FileType type;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "file_meta", joinColumns = {@JoinColumn(name = "file_id", referencedColumnName = "id")})
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @Getter @Setter
    private Map<String, String> fileMeta;

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

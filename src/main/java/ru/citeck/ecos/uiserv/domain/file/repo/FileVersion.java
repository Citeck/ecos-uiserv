package ru.citeck.ecos.uiserv.domain.file.repo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import ru.citeck.ecos.uiserv.domain.translation.repo.Translated;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A MenuConfigVersion.
 */
@Deprecated
@Entity
@Table(name = "file_version")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Data
public class FileVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    private Long id;

    /* This may contain null. For standard updates, this indicates that the menu is no longer
        considered existing since that version becomes active, like is't not in File table.
        For custom overrides, this means the same thing, but of course custom override
        can be reverted to latest standard version. */
    @Type(type="org.hibernate.type.BinaryType")
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


    public FileVersion translated(Translated translated) {
        this.translated = translated;
        return this;
    }

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
}

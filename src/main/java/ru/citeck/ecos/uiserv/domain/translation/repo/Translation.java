package ru.citeck.ecos.uiserv.domain.translation.repo;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * A Translation.
 */
@Deprecated
@Entity
@Table(name = "translation")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Translation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "lang_tag", nullable = false)
    private String langTag;


    @Lob
    @Column(name = "bundle", nullable = false)
    private byte[] bundle;

    @ManyToOne
    @JsonIgnoreProperties("translations")
    private Translated translated;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLangTag() {
        return langTag;
    }

    public void setLangTag(String langTag) {
        this.langTag = langTag;
    }

    public byte[] getBundle() {
        return bundle;
    }

    public void setBundle(byte[] bundle) {
        this.bundle = bundle;
    }

    public Translated getTranslated() {
        return translated;
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
        Translation translation = (Translation) o;
        if (translation.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), translation.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Translation{" +
            "id=" + getId() +
            ", langTag='" + getLangTag() + "'" +
            "}";
    }
}

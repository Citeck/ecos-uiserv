package ru.citeck.ecos.uiserv.domain.translation.repo;


import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * A Translated.
 */
@Deprecated
@Entity
@Table(name = "translated")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Translated implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Translated translated = (Translated) o;
        if (translated.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), translated.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Translated{" +
            "id=" + getId() +
            "}";
    }
}

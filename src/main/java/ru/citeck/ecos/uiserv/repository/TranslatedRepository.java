package ru.citeck.ecos.uiserv.repository;

import ru.citeck.ecos.uiserv.domain.Translated;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Translated entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TranslatedRepository extends JpaRepository<Translated, Long> {

}

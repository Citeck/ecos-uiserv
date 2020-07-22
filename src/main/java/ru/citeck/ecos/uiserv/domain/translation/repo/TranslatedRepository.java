package ru.citeck.ecos.uiserv.domain.translation.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Translated entity.
 */
@Deprecated
@SuppressWarnings("unused")
@Repository
public interface TranslatedRepository extends JpaRepository<Translated, Long> {

}

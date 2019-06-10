package ru.citeck.ecos.uiserv.repository;

import ru.citeck.ecos.uiserv.domain.Translated;
import ru.citeck.ecos.uiserv.domain.Translation;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * Spring Data  repository for the Translation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    Optional<Translation> findOneByTranslatedIdAndLangTag(Long translatedId, String s);
}

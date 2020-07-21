package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.Translation;

import java.util.List;
import java.util.Optional;


/**
 * Spring Data  repository for the Translation entity.
 */
@Deprecated
@SuppressWarnings("unused")
@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    Optional<Translation> findOneByTranslatedIdAndLangTag(Long translatedId, String s);

    List<Translation> findAllByTranslatedId(Long translatedId);
}

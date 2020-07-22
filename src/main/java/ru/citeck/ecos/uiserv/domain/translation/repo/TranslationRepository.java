package ru.citeck.ecos.uiserv.domain.translation.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

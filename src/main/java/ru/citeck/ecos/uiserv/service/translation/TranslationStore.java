package ru.citeck.ecos.uiserv.service.translation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.Translated;
import ru.citeck.ecos.uiserv.domain.Translation;
import ru.citeck.ecos.uiserv.repository.TranslatedRepository;
import ru.citeck.ecos.uiserv.repository.TranslationRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

@Component
public class TranslationStore {

    @Autowired
    private TranslationRepository translationRepository;

    @Autowired
    private TranslatedRepository translatedRepository;

    public Optional<byte[]> loadBundle(Long translatedEntityId, String languageTag) {
        final Optional<Translation> props = translationRepository.findOneByTranslatedIdAndLangTag(
            translatedEntityId, languageTag);
        return props.map(Translation::getBundle);
    }

    public void storeBundle(Long translatedEntityId, String languageTag, byte[] props) {
        final Translation t = translationRepository.findOneByTranslatedIdAndLangTag(translatedEntityId, languageTag)
            .orElse(new Translation());
        t.setTranslated(translatedRepository.getOne(translatedEntityId));
        t.setBundle(props);
        t.setLangTag(languageTag);
        translationRepository.save(t);
    }
}

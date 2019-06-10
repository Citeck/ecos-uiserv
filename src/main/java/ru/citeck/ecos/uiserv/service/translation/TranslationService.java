package ru.citeck.ecos.uiserv.service.translation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.uiserv.service.CachedTranslationStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

@Service
public class TranslationService {
    @Autowired
    private CachedTranslationStore translationStore;

    public Optional<ResourceBundle> getTranslations(Long translatedEntityId, Locale locale) {
        return translationStore.loadBundle(translatedEntityId,
            locale.toLanguageTag().split("-", 2)[0])
            .map(this::toBundle);
    }

    public void saveTranslations(Long translatedEntityId, String tag, byte[] props) {
        translationStore.storeBundle(translatedEntityId, tag, props);
    }

    public ResourceBundle toBundle(byte[] data) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            return new PropertyResourceBundle(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String pickLocalizedString(Map<String, String> localizedTitle, Locale locale) {
        final String specific = localizedTitle.get(locale.toLanguageTag().split("-", 2)[0]);
        if (specific != null)
            return specific;
        final String english = localizedTitle.get(Locale.ENGLISH.toLanguageTag().split("-", 2)[0]);
        if (english != null)
            return english;
        return localizedTitle.values().iterator().next();
    }
}

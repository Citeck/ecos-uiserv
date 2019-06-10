package ru.citeck.ecos.uiserv.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.service.translation.TranslationStore;

import java.util.Optional;

@Component
//todo Внимание, для обычного кэша по умолчанию (concurrenthashmap) нет eviction policy!
@CacheConfig(cacheNames={"translations"})
public class CachedTranslationStore {
    private final static String KEY = "{#translatedEntityId, #languageTag}";

    @Autowired
    private TranslationStore store;

    @Cacheable(key = KEY)
    public Optional<byte[]> loadBundle(Long translatedEntityId, String languageTag) {
        return store.loadBundle(translatedEntityId, languageTag);
    }

    @CachePut(key = KEY)
    public Optional<byte[]> storeBundle(Long translatedEntityId, String languageTag, byte[] props) {
        store.storeBundle(translatedEntityId, languageTag, props);
        return Optional.of(props);
    }
}

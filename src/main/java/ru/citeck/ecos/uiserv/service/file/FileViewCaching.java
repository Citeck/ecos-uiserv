package ru.citeck.ecos.uiserv.service.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.uiserv.domain.File;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Deprecated
public class FileViewCaching<TView> {
    private final Map<String, Optional<Versioned<TView>>> cache = new ConcurrentHashMap<>();
    private final Function<String, Optional<File>> fetcher;
    private final Function<File, Optional<TView>> mapper;

    public FileViewCaching(Function<String, Optional<File>> fetcher, Function<File, Optional<TView>> mapper) {
        this.fetcher = fetcher;
        this.mapper = mapper;
    }

    public Optional<TView> get(String menuId) {
        final Optional<Versioned<TView>> ours = cache.getOrDefault(menuId, Optional.empty());
        final Optional<File> theirs = fetcher.apply(menuId);
        if (ours.map(Versioned::getVersionId)
            .flatMap(x -> theirs.map(File::getFileVersion).map(x::equals))
            .orElse(false))
            return ours.map(Versioned::getView);
        final Optional<Versioned<TView>> update = theirs
            .flatMap(x -> mapper.apply(x).map(target ->
                new Versioned<>(x.getFileVersion().getId(), target)));
        cache.put(menuId, update);
        return update.map(Versioned::getView);
    }

    @Getter @AllArgsConstructor
    private static class Versioned<TView> {
        private final Long versionId;
        private final TView view;
    }
}

package ru.citeck.ecos.uiserv.domain.journal.service;

import kotlin.Pair;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.entity.EntityWithMeta;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.service.mapper.JournalMapper;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;
import ru.citeck.ecos.uiserv.domain.journal.service.provider.JournalsProvider;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers.JournalsResolver;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final Pattern VALID_COLUMN_NAME_PATTERN = Pattern.compile("^\\d?[a-zA-Z_][$\\da-zA-Z:_-]*$");
    private final Pattern VALID_COLUMN_ATT_PATTERN = Pattern.compile("^[a-zA-Z_][$.\\da-zA-Z:_-]*$");

    private final JournalRepository journalRepository;
    private final JournalMapper journalMapper;

    private final JpaSearchConverterFactory jpaSearchConverterFactory;
    private JpaSearchConverter<JournalEntity> searchConv;

    private final List<BiConsumer<JournalWithMeta, JournalWithMeta>> changeListeners = new CopyOnWriteArrayList<>();

    private final Map<String, JournalsProvider> providers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        searchConv = jpaSearchConverterFactory.createConverter(JournalEntity.class).build();
    }

    public long getLastModifiedTimeMs() {
        return journalRepository.getLastModifiedTime()
            .map(Instant::toEpochMilli)
            .orElse(0L);
    }

    @Override
    public JournalWithMeta getJournalById(String id) {
        if (id.contains("$")) {
            String providerId = id.substring(0, id.indexOf('$'));
            if (providers.containsKey(providerId)) {
                JournalsProvider journalsProvider = providers.get(providerId);
                EntityWithMeta<JournalDef> journal =
                    journalsProvider.getJournalById(id.substring(id.indexOf('$') + 1));

                if (journal == null) {
                    return null;
                }
                JournalDef journalDef = journal.getEntity();
                if (journalDef.getId().isEmpty()) {
                    journalDef = journalDef.copy().withId(id).build();
                }
                JournalWithMeta journalWithMeta = new JournalWithMeta(journalDef);
                journalWithMeta.setCreated(journal.getMeta().getCreated());
                journalWithMeta.setCreator(journal.getMeta().getCreator());
                journalWithMeta.setModified(journal.getMeta().getModified());
                journalWithMeta.setModifier(journal.getMeta().getModifier());
                return journalWithMeta;
            }
        }
        Optional<JournalEntity> entity = journalRepository.findByExtId(id);
        return entity.map(journalMapper::entityToDto).orElse(null);
    }

    @Override
    public List<JournalWithMeta> getAll(Predicate predicate, int max, int skip, List<SortBy> sort) {
        return searchConv.findAll(journalRepository, predicate, max, skip, sort)
            .stream()
            .map(journalMapper::entityToDto)
            .collect(Collectors.toList());
    }

    @Override
    public Set<JournalWithMeta> getAll(int maxItems, int skipCount) {

        if (maxItems <= 0) {
            return Collections.emptySet();
        }

        PageRequest page = PageRequest.of(
            skipCount / maxItems,
            maxItems,
            Sort.by(Sort.Direction.DESC, "id")
        );

        return journalRepository.findAll(page).stream()
            .map(journalMapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public List<JournalWithMeta> getAll(Collection<String> extIds) {

        Map<String, Integer> idsToRequestFromDb = new HashMap<>();
        List<Pair<JournalWithMeta, Integer>> result = new ArrayList<>();
        int idx = 0;
        for (String id : extIds) {
            if (id.contains("$")) {
                String providerId = id.substring(0, id.indexOf('$'));
                if (providers.containsKey(providerId)) {
                    JournalWithMeta journalById = getJournalById(id);
                    if (journalById != null) {
                        result.add(new Pair<>(journalById, idx));
                    }
                } else {
                    idsToRequestFromDb.put(id, idx);
                }
            } else {
                idsToRequestFromDb.put(id, idx);
            }
            idx++;
        }

        journalRepository.findAllByExtIdIn(idsToRequestFromDb.keySet()).stream()
            .map(journalMapper::entityToDto)
            .forEach(v ->
                result.add(new Pair<>(v, idsToRequestFromDb.getOrDefault(v.getJournalDef().getId(), 0))));

        result.sort(Comparator.comparing(Pair<JournalWithMeta, Integer>::getSecond));

        return result.stream()
            .map(Pair::getFirst)
            .collect(Collectors.toList());
    }

    @Override
    public long getCount() {
        return (int) journalRepository.count();
    }

    @Override
    public long getCount(Predicate predicate) {
        return searchConv.getCount(journalRepository, predicate);
    }

    @Override
    public void onJournalWithMetaChanged(BiConsumer<JournalWithMeta, JournalWithMeta> consumer) {
        changeListeners.add(consumer);
    }

    @Override
    public void onJournalChanged(BiConsumer<JournalDef, JournalDef> consumer) {
        changeListeners.add((before, after) -> {
            JournalDef beforeDef = null;
            if (before != null) {
                beforeDef = before.getJournalDef();
            }
            JournalDef afterDef = null;
            if (after != null) {
                afterDef = after.getJournalDef();
            }
            consumer.accept(beforeDef, afterDef);
        });
    }

    @Override
    public JournalWithMeta save(JournalDef dto) {

        if (dto.getId().isEmpty() || dto.getId().contains("$")) {
            throw new IllegalArgumentException("Journal without id: " + dto);
        }

        // preprocess config with builder
        dto = dto.copy().build();

        dto.getColumns().forEach(column -> {
            Matcher validNameMatcher = VALID_COLUMN_NAME_PATTERN.matcher(column.getId());
            if (!validNameMatcher.matches()) {
                throw new IllegalArgumentException(
                    "Journal column name is invalid: '" + column.getId() + "'. Column: " + column);
            }
            if (StringUtils.isNotBlank(column.getAttribute())) {
                Matcher validAttMatcher = VALID_COLUMN_ATT_PATTERN.matcher(column.getAttribute());
                if (!validAttMatcher.matches()) {
                    throw new IllegalArgumentException(
                        "Journal column attribute is invalid: '" + column.getAttribute() + "'. Column: " + column);
                }
            }
        });

        JournalWithMeta valueBefore = journalRepository.findByExtId(dto.getId())
            .map(journalMapper::entityToDto)
            .orElse(null);

        JournalEntity journalEntity = journalMapper.dtoToEntity(dto);
        JournalEntity storedJournalEntity = journalRepository.save(journalEntity);

        JournalWithMeta journalDto = journalMapper.entityToDto(storedJournalEntity);

        for (BiConsumer<JournalWithMeta, JournalWithMeta> listener : changeListeners) {
            listener.accept(valueBefore, journalDto);
        }
        return journalDto;
    }

    @Override
    public void delete(String id) {
        journalRepository.findByExtId(id).ifPresent(journalRepository::delete);
    }

    @Override
    public void registerProvider(JournalsProvider provider) {
        this.providers.put(provider.getType(), provider);
    }
}

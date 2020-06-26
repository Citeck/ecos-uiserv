package ru.citeck.ecos.uiserv.journal.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.journal.domain.JournalEntity;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.journal.repository.JournalRepository;
import ru.citeck.ecos.uiserv.journal.mapper.JournalMapper;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@DependsOn({"liquibase", "recordsServiceFactoryConfiguration"})
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;
    private final JournalMapper journalMapper;

    private Consumer<JournalDto> changeListener;

    private final Map<String, Set<String>> typesByJournalListId = new ConcurrentHashMap<>();
    private final Map<String, String> journalListByTypeId = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        journalRepository.findAll().forEach(journal ->
            updateJournalLists(journalMapper.entityToDto(journal))
        );
    }

    public long getLastModifiedTimeMs() {
        return journalRepository.getLastModifiedTime()
            .map(Instant::toEpochMilli)
            .orElse(0L);
    }

    @Override
    public JournalWithMeta getJournalById(String id) {
        Optional<JournalEntity> entity = journalRepository.findByExtId(id);
        return entity.map(journalMapper::entityToDto).orElse(null);
    }

    @Override
    public List<JournalWithMeta> getAll(int max, int skipCount, Predicate predicate) {

        PageRequest page = PageRequest.of(
            skipCount / max,
            max,
            Sort.by(Sort.Direction.DESC, "id")
        );

        return journalRepository.findAll(toSpec(predicate), page)
            .stream()
            .map(journalMapper::entityToDto)
            .collect(Collectors.toList());
    }

    @Override
    public JournalWithMeta getById(String id) {
        JournalEntity journal = journalRepository.findByExtId(id).orElse(null);
        if (journal == null) {
            return null;
        }
        return journalMapper.entityToDto(journal);
    }

    @Override
    public Set<JournalWithMeta> getAll(int maxItems, int skipCount) {

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
    public Set<JournalWithMeta> getAll(Set<String> extIds) {
        return journalRepository.findAllByExtIdIn(extIds).stream()
            .map(journalMapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public long getCount() {
        return (int) journalRepository.count();
    }

    @Override
    public long getCount(Predicate predicate) {
        Specification<JournalEntity> spec = toSpec(predicate);
        return spec != null ? (int) journalRepository.count(spec) : getCount();
    }

    @Override
    public void onJournalChanged(Consumer<JournalDto> consumer) {
        changeListener = consumer;
    }

    @Override
    public JournalWithMeta save(JournalDto dto) {

        if (dto.getColumns() == null
            || dto.getColumns().stream().noneMatch(c -> StringUtils.isNotBlank(c.getName()))) {

            throw new IllegalStateException("Columns is a mandatory for journal config");
        }

        JournalEntity journalEntity = journalMapper.dtoToEntity(dto);
        JournalEntity storedJournalEntity = journalRepository.save(journalEntity);

        JournalWithMeta journalDto = journalMapper.entityToDto(storedJournalEntity);

        updateJournalLists(journalDto);

        changeListener.accept(journalDto);
        return journalDto;
    }

    @Override
    public void delete(String id) {
        journalRepository.findByExtId(id).ifPresent(journalRepository::delete);
    }

    private Specification<JournalEntity> toSpec(Predicate predicate) {

        PredicateDto predicateDto = PredicateUtils.convertToDto(predicate, PredicateDto.class);
        Specification<JournalEntity> spec = null;

        if (StringUtils.isNotBlank(predicateDto.name)) {
            spec = (root, query, builder) ->
                builder.like(builder.lower(root.get("name")), "%" + predicateDto.name.toLowerCase() + "%");
        }
        if (StringUtils.isNotBlank(predicateDto.moduleId)) {
            Specification<JournalEntity> idSpec = (root, query, builder) ->
                builder.like(builder.lower(root.get("extId")), "%" + predicateDto.moduleId.toLowerCase() + "%");
            spec = spec != null ? spec.or(idSpec) : idSpec;
        }

        return spec;
    }

    @Override
    public List<JournalWithMeta> getJournalsByJournalList(String journalListId) {

        if (StringUtils.isBlank(journalListId)) {
            return Collections.emptyList();
        }

        return typesByJournalListId.getOrDefault(journalListId, Collections.emptySet())
            .stream()
            .map(journalRepository::findByExtId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(journalMapper::entityToDto)
            .collect(Collectors.toList());
    }

    private synchronized void updateJournalLists(JournalDto journalDto) {

        ObjectData attributes = journalDto.getAttributes();
        String listId = attributes != null ? attributes.get("journalsListId").asText() : "";

        if (StringUtils.isNotBlank(listId)) {

            Set<String> listJournals = typesByJournalListId.computeIfAbsent(listId, id ->
                Collections.newSetFromMap(new ConcurrentHashMap<>())
            );

            String listIdBefore = journalListByTypeId.get(journalDto.getId());
            if (StringUtils.isNotBlank(listIdBefore)) {
                if (!listIdBefore.equals(listId)) {
                    Set<String> listBefore = typesByJournalListId.computeIfAbsent(listIdBefore, id ->
                        Collections.newSetFromMap(new ConcurrentHashMap<>())
                    );
                    listBefore.remove(journalDto.getId());
                    listJournals.add(journalDto.getId());
                }
            } else {
                listJournals.add(journalDto.getId());
            }

            journalListByTypeId.put(journalDto.getId(), listId);

        } else {

            String listIdBefore = journalListByTypeId.get(journalDto.getId());

            if (StringUtils.isNotBlank(listIdBefore)) {

                typesByJournalListId.computeIfAbsent(listIdBefore, id ->
                    Collections.newSetFromMap(new ConcurrentHashMap<>())
                ).remove(journalDto.getId());

                journalListByTypeId.remove(journalDto.getId());
            }
        }
    }

    @Data
    public static class PredicateDto {
        private String name;
        private String moduleId;
    }
}

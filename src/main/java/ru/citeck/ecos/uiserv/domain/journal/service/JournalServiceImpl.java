package ru.citeck.ecos.uiserv.domain.journal.service;

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
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.service.mapper.JournalMapper;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;

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

    private final Map<String, Set<String>> journalsByJournalListId = new ConcurrentHashMap<>();
    private final Map<String, String> journalListByJournalId = new ConcurrentHashMap<>();

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

            throw new IllegalStateException("Columns is a mandatory for journal config. Journal: " + dto);
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

        if (StringUtils.isNotBlank(predicateDto.label)) {
            spec = (root, query, builder) ->
                builder.like(builder.lower(root.get("label")), "%" + predicateDto.label.toLowerCase() + "%");
        }
        if (StringUtils.isNotBlank(predicateDto.moduleId)) {
            Specification<JournalEntity> idSpec = (root, query, builder) ->
                builder.like(builder.lower(root.get("extId")), "%" + predicateDto.moduleId.toLowerCase() + "%");
            spec = spec != null ? spec.or(idSpec) : idSpec;
        }

        return spec;
    }

    @Override
    public List<JournalWithMeta> getJournalsWithSite() {
        Set<String> journalIds = new HashSet<>();
        journalsByJournalListId.forEach((listId, journals) -> {
            if (listId.startsWith("site-") && listId.endsWith("-main")) {
                journalIds.addAll(journals);
            }
        });
        return journalIds.stream()
            .map(journalRepository::findByExtId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(journalMapper::entityToDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<JournalWithMeta> getJournalsByJournalList(String journalListId) {

        if (StringUtils.isBlank(journalListId)) {
            return Collections.emptyList();
        }

        return journalsByJournalListId.getOrDefault(journalListId, Collections.emptySet())
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

            Set<String> listJournals = journalsByJournalListId.computeIfAbsent(listId, id ->
                Collections.newSetFromMap(new ConcurrentHashMap<>())
            );

            String listIdBefore = journalListByJournalId.get(journalDto.getId());
            if (StringUtils.isNotBlank(listIdBefore)) {
                if (!listIdBefore.equals(listId)) {
                    Set<String> listBefore = journalsByJournalListId.computeIfAbsent(listIdBefore, id ->
                        Collections.newSetFromMap(new ConcurrentHashMap<>())
                    );
                    listBefore.remove(journalDto.getId());
                    listJournals.add(journalDto.getId());
                }
            } else {
                listJournals.add(journalDto.getId());
            }

            journalListByJournalId.put(journalDto.getId(), listId);

        } else {

            String listIdBefore = journalListByJournalId.get(journalDto.getId());

            if (StringUtils.isNotBlank(listIdBefore)) {

                journalsByJournalListId.computeIfAbsent(listIdBefore, id ->
                    Collections.newSetFromMap(new ConcurrentHashMap<>())
                ).remove(journalDto.getId());

                journalListByJournalId.remove(journalDto.getId());
            }
        }
    }

    @Data
    public static class PredicateDto {
        private String label;
        private String moduleId;
    }
}

package ru.citeck.ecos.uiserv.journal.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.journal.domain.JournalEntity;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.repository.JournalRepository;
import ru.citeck.ecos.uiserv.journal.mapper.JournalMapper;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@DependsOn("liquibase")
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;
    private final JournalMapper journalMapper;
    private final RecordsService recordsService;

    private Consumer<JournalDto> changeListener;

    @Override
    public JournalDto getJournalById(String id) {
        Optional<JournalEntity> entity = journalRepository.findByExtId(id);
        return entity.map(journalMapper::entityToDto).orElse(null);
    }

    @Override
    public Set<JournalDto> getAll(int max, int skipCount, Predicate predicate) {

        PageRequest page = PageRequest.of(
            skipCount / max,
            max,
            Sort.by(Sort.Direction.DESC, "id")
        );

        return journalRepository.findAll(toSpec(predicate), page)
            .stream()
            .map(journalMapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public JournalDto getById(String id) {
        JournalEntity journal = journalRepository.findByExtId(id).orElse(null);
        if (journal == null) {
            return null;
        }
        return journalMapper.entityToDto(journal);
    }

    @Override
    public Set<JournalDto> getAll(int maxItems, int skipCount) {

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
    public Set<JournalDto> getAll(Set<String> extIds) {
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
    public JournalDto save(JournalDto dto) {

        JournalEntity journalEntity = journalMapper.dtoToEntity(dto);
        JournalEntity storedJournalEntity = journalRepository.save(journalEntity);

        JournalDto journalDto = journalMapper.entityToDto(storedJournalEntity);

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

    @Data
    public static class PredicateDto {
        private String name;
        private String moduleId;
    }
}

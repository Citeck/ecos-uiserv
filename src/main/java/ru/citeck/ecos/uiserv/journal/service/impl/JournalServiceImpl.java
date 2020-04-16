package ru.citeck.ecos.uiserv.journal.service.impl;

import lombok.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.journal.domain.JournalEntity;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.repository.JournalRepository;
import ru.citeck.ecos.uiserv.service.exception.ResourceNotFoundException;
import ru.citeck.ecos.uiserv.journal.service.JournalService;
import ru.citeck.ecos.uiserv.journal.mapper.JournalMapper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;
    private final JournalMapper journalMapper;
    private final RecordsService recordsService;

    private Consumer<JournalDto> changeListener;

    @Override
    public Set<JournalDto> getAll(int max, int skipCount, Predicate predicate) {
        PageRequest page = PageRequest.of(skipCount / max, max, Sort.by(Sort.Direction.DESC, "id"));

        return journalRepository.findAll(toSpec(predicate), page)
            .stream()
            .map(journalMapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public JournalDto getById(String id) {

        JournalEntity journal = journalRepository.findByExtId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Journal", "id", id));

        return journalMapper.entityToDto(journal);
    }

    @Override
    public Set<JournalDto> getAll(int maxItems, int skipCount) {
        PageRequest page = PageRequest.of(skipCount / maxItems, maxItems, Sort.by(Sort.Direction.DESC, "id"));

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
    public JournalDto update(JournalDto dto) {
        JournalEntity journalEntity = journalMapper.dtoToEntity(dto);
        JournalEntity storedJournalEntity = journalRepository.save(journalEntity);
        JournalDto journalDto = journalMapper.entityToDto(storedJournalEntity);
        changeListener.accept(journalDto);
        return journalDto;
    }

    /**
     * Search journal by 2 ways:
     * 1. in type hierarchy
     * 2. searching journal with same typeRef
     *
     * @param typeRef - input value of typeRef
     * @return journalDto - result of search
     */
    @Override
    @Nullable
    public JournalDto searchJournalByTypeRef(@NonNull RecordRef typeRef) {

        TypeJournalMeta typeJournalResult = recordsService.getMeta(typeRef, TypeJournalMeta.class);

        List<RecordRef> parentsTypeRefs = typeJournalResult.getParentsRefs();
        RecordRef journalRef = typeJournalResult.getJournalRef();

        JournalEntity journalWithSameTypeRef = getByTypeRef(typeRef.toString());

        if (journalRef == null && CollectionUtils.isNotEmpty(parentsTypeRefs)) {

            //  search by types hierarchy iteration
            for (RecordRef parentTypeRef : parentsTypeRefs) {

                TypeJournalMeta parentTypeJournalResult = recordsService.getMeta(parentTypeRef, TypeJournalMeta.class);
                journalRef = parentTypeJournalResult.getJournalRef();

                if (journalRef == null && journalWithSameTypeRef == null) {
                    // for case, when we search JournalEntity with same TypeRef
                    journalWithSameTypeRef = getByTypeRef(parentTypeRef.toString());
                }

                if (journalRef != null) {
                    break;
                }
            }
        }

        if (journalRef == null && journalWithSameTypeRef != null) {
            //  JournalEntity with same TypeRef
            return journalMapper.entityToDto(journalWithSameTypeRef);
        }

        if (journalRef != null) {
            JournalEntity journalEntity = journalRepository.findByExtId(journalRef.getId()).orElse(null);
            if (journalEntity != null) {
                return journalMapper.entityToDto(journalEntity);
            }
        }

        return null;
    }

    private JournalEntity getByTypeRef(String typeRefStr) {
        Optional<JournalEntity> optionalJournalEntity = journalRepository.findAllByTypeRef(typeRefStr).stream()
            .findFirst();
        return optionalJournalEntity.orElse(null);
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TypeJournalMeta {
        @MetaAtt("journal?str")
        private RecordRef journalRef;

        @MetaAtt("parents?str")
        private List<RecordRef> parentsRefs;
    }
}

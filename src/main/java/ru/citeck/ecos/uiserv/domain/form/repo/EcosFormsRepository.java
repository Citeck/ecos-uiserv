package ru.citeck.ecos.uiserv.domain.form.repo;

import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@Repository
public interface EcosFormsRepository extends JpaRepository<EcosFormEntity, Long>,
                                             JpaSpecificationExecutor<EcosFormEntity> {

    List<EcosFormEntity> findAllByTypeRef(String type);

    List<EcosFormEntity> findAllByTypeRefIn(List<String> types);

    Set<EcosFormEntity> findAllByExtIdIn(Set<String> ids);

    @Nullable
    EcosFormEntity findByExtId(String extId);

    @Nullable
    EcosFormEntity findFirstByFormKey(String formKey);
}

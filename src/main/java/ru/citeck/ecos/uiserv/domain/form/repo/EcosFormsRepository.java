package ru.citeck.ecos.uiserv.domain.form.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface EcosFormsRepository extends JpaRepository<EcosFormEntity, Long>,
                                             JpaSpecificationExecutor<EcosFormEntity> {

    List<EcosFormEntity> findAllByTypeRef(String type);

    List<EcosFormEntity> findAllByTypeRefIn(List<String> types);

    Optional<EcosFormEntity> findByExtId(String extId);

    Optional<EcosFormEntity> findFirstByFormKey(String formKey);
}

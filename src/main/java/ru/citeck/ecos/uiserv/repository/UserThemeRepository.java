package ru.citeck.ecos.uiserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.domain.UserTheme;

import javax.persistence.LockModeType;
import java.util.Optional;


@SuppressWarnings("unused")
@Repository
public interface UserThemeRepository extends JpaRepository<UserTheme, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) //todo try optimistic lock instead of pessimistic
    Optional<UserTheme> findByUserNameAndSiteId(String userName, String siteId);
}

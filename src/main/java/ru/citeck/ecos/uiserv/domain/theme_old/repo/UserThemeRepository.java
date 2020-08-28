package ru.citeck.ecos.uiserv.domain.theme_old.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface UserThemeRepository extends JpaRepository<UserTheme, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) //todo try optimistic lock instead of pessimistic
    Optional<UserTheme> findByUserNameAndSiteId(String userName, String siteId);
}

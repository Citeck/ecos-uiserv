package ru.citeck.ecos.uiserv.domain.theme.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.uiserv.app.application.props.ApplicationProperties;
import ru.citeck.ecos.uiserv.domain.file.repo.File;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.theme.repo.UserTheme;
import ru.citeck.ecos.uiserv.domain.theme.repo.UserThemeRepository;
import ru.citeck.ecos.uiserv.domain.file.service.FileService;

@Service
@RequiredArgsConstructor
@Transactional
public class ThemeService {
    private final FileService fileService;
    private final UserThemeRepository userThemeRepository;
    private final ApplicationProperties applicationProperties;

    private final static String GLOBAL_THEME_NAME = "*";

    public LogoFile getLogoOverride(String username) {
        return fileService.loadFile(FileType.LOGO, "themes-logo@" + username)
            .map(LogoFile::new).orElse(null);
    }

    public void setLogoOverride(String username, String contentType, byte[] bytes) {
        fileService.deployFileOverride(FileType.LOGO, "themes-logo@" + username, contentType, bytes, null);
    }

    public LogoFile getMobileLogoOverride(String username) {
        return fileService.loadFile(FileType.LOGO, "themes-mlogo@" + username)
            .map(LogoFile::new).orElse(null);
    }

    public void setMobileLogoOverride(String username, String contentType, byte[] bytes) {
        fileService.deployFileOverride(FileType.LOGO, "themes-mlogo@" + username, contentType, bytes, null);
    }

    public void saveThemeId(String username, String siteId, String themeId) {
        if (GLOBAL_THEME_NAME.equals(siteId)) {
            throw new IllegalArgumentException("Not a valid site-id: " + GLOBAL_THEME_NAME);
        }
        if (siteId == null) {
            siteId = GLOBAL_THEME_NAME;
        }
        final UserTheme userTheme = userThemeRepository.findByUserNameAndSiteId(username, siteId)
            .orElseGet(UserTheme::new);
        userTheme.setThemeId(themeId);
        userTheme.setSiteId(siteId);
        userTheme.setUserName(username);
        userThemeRepository.save(userTheme);
    }

    public String getThemeId(String username, String siteId) {
        if (GLOBAL_THEME_NAME.equals(siteId)) {
            throw new IllegalArgumentException("Not a valid site-id: " + GLOBAL_THEME_NAME);
        }
        if (siteId == null) {
            siteId = GLOBAL_THEME_NAME;
        }
        return userThemeRepository.findByUserNameAndSiteId(username, siteId)
            .map(UserTheme::getThemeId)
            .orElse(applicationProperties.getDefaultThemeId());
    }

    @Getter
    public static class LogoFile {
        private final byte[] bytes;
        private final Long versionId;
        private final String contentType;

        public LogoFile(File file) {
            this.bytes = file.getFileVersion().getBytes();
            this.versionId = file.getFileVersion().getId();
            this.contentType = file.getFileVersion().getContentType();
        }
    }
}

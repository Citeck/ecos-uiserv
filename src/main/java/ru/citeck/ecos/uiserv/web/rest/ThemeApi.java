package ru.citeck.ecos.uiserv.web.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import ru.citeck.ecos.uiserv.service.ThemeService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@RestController
//we also serve theme's content (CSS, images etc.) although not with this controller,
// but as static content. This is to be changed, we probably want to store theme files in DB,
// to be able to make changes and deploy updates w/o modifying the application.
@RequestMapping("/api/themes")
@Transactional
public class ThemeApi {
    @Autowired
    private ThemeService themeService;

    //by default, Spring serves static resources right at context root
    //format argument is theme id, that's why only alphanumeric characters are accepted as theme id by uiserv
    private final static String STATIC_PATH = "themes/%s/images/";

    //If logo is queried several times per page loading, having this to be nonzero might be good;
    //  at the same time, 1 second delay before browser sees updates to the logo is likely noncritical
    //  (we can even make client wait for 1 extra second when it sends updates).
    //But, let's not optimize prematurely, and stick to zero instead.
    private final static Long ALLOWED_STALE_SECONDS = 0L;

    private final static String OVERRIDE_PREFIX = "087ff03f-854d-4965-9642-7625299d6b87:";

    @GetMapping("/form")
    public String getDebugForm() {
        return "<form action=\"/api/themes/combined?alfUsername3=admin\" method=\"post\" enctype=\"multipart/form-data\">\n" +
            "Current logo: <img src=\"/api/themes/logoOverride?alfUsername3=admin\"/><br/>\n" +
            "    Select image to upload:<br/>\n" +
            //"    <input type=\"text\" name=\"themeId\" id=\"themeId\">\n" +
            "    Logo <input type=\"file\" name=\"logoOverride\" id=\"logoOverride\"><br/>\n" +
            "    MLogo <input type=\"file\" name=\"mobileLogoOverride\" id=\"mobileLogoOverride\">\n" +
            "    <input type=\"submit\" value=\"Upload Image\" name=\"submit\">\n" +
            "</form>\n";
    }

    @PostMapping("/combined")
    public void saveCombinedConfigForm(@ModelAttribute("username") String username,
                                       @RequestParam(required = false) String themeId,
                                       @RequestParam(required = false) String siteId,
                                       @RequestParam(required = false) MultipartFile logoOverride,
                                       @RequestParam(required = false) MultipartFile mobileLogoOverride) {
        if (username == null)
            throw new IllegalArgumentException("Expecting username");
        if (logoOverride != null && !logoOverride.isEmpty()) {
            try {
                themeService.setLogoOverride(username, logoOverride.getContentType(), logoOverride.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Unable to set logo override", e);
            }
        }
        if (mobileLogoOverride != null && !mobileLogoOverride.isEmpty()) {
            try {
                themeService.setMobileLogoOverride(username, mobileLogoOverride.getContentType(), mobileLogoOverride.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Unable to set mobile logo override", e);
            }
        }
        if (themeId != null) {
            if ("".equals(siteId)) {
                siteId = null;
            }
            themeService.saveThemeId(username, siteId, themeId);
        }
    }

    //Returns the override previously passed into /combined; 404 if none was
    @GetMapping("/logoOverride")
    public ResponseEntity<byte[]> getLogoOverride(@ModelAttribute("username") String username,
                                                  @RequestHeader(value = "If-None-Match", required = false) String eTag) throws IOException {
        return genericGetLogoOverride(() -> themeService.getLogoOverride(username), eTag);
    }

    //Returns the override previously passed into /combined; 404 if none was
    @GetMapping("/mobileLogoOverride")
    public ResponseEntity<byte[]> getMobileLogoOverride(@ModelAttribute("username") String username,
                                                        @RequestHeader(value = "If-None-Match", required = false) String eTag) throws IOException {
        return genericGetLogoOverride(() -> themeService.getMobileLogoOverride(username), eTag);
    }

    private ResponseEntity<byte[]> genericGetLogoOverride(Supplier<ThemeService.LogoFile> logoLoader,
                                                          String requestETag) {
        //todo don't eager-load bytes[] unless we're sure it's not status 304
        final ThemeService.LogoFile logoFile = logoLoader.get();

        if (logoFile == null)
            return ResponseEntity.notFound().build();

        //The idea is, ETag is meant to be made in such a way as to never collide with
        //  some other ETag generated by other branch of execution of logo-fetching requests.
        //See how /logo acts as a dispatcher; it may forward request to be processed by this code,
        //  even if the ETag provided by the caller was assigned by different branch of execution,
        //  not by this code. And in that case that tag must not be equal to any tag this code could
        //  generate, in order the request not to be short-circuited with NOT_MODIFIED; and vice versa,
        //  unique prefix guarantees that the tag we make will not later be accepted as valid
        //  by different branch of execution.
        final String responseETag = OVERRIDE_PREFIX + logoFile.getVersionId().toString();

        if(requestETag != null && ("*".equals(requestETag) || responseETag.equals(requestETag) )) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .cacheControl(CacheControl.maxAge(ALLOWED_STALE_SECONDS, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .eTag(responseETag)
                .build();
        } else {
            final MediaType contentType = logoFile.getContentType() != null ?
                MediaType.parseMediaType(logoFile.getContentType()) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(ALLOWED_STALE_SECONDS, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .eTag(responseETag)
                .contentType(contentType)
                .body(logoFile.getBytes());
        }
    }

    //The idea is simple but the implementation is somewhat tricky because of ETag support: the method
    //  may be given ETag generated by one of its possible forwards, but must only be actually used
    //  if that forward is chosen again this time, and not some other forward. We solve this by making
    //  ETags produced by different forwards incompatible.
    //Returns the override previously passed into /combined; if none was, returns non-overridden logo from theme
    @GetMapping("/logo/{themeId}")
    public ModelAndView getLogo(@ModelAttribute("username") String username, @PathVariable String themeId) {
        if (username == null)
            throw new IllegalArgumentException("Expecting username");
        //even if we're going to call /logoOverride, we are doing it in the same transaction, so Level-1 cache
        //  will prevent excess query when /logoOverride in turn calls getLogoOverride (again)
        //todo add method to extract logo override and current theme together
        return new ModelAndView(themeService.getLogoOverride(username) != null ?
            "forward:logoOverride" : "forward:/" + String.format(STATIC_PATH, themeId) +
            "app-logo-48.png");
    }

    //Returns the override previously passed into /combined; if none was, returns non-overridden logo from theme
    @GetMapping("/moblogo/{themeId}")
    public ModelAndView getMobileLogo(@ModelAttribute("username") String username, @PathVariable String themeId) {
        //even if we're going to call /logoOverride, we are doing it in the same transaction, so Level-1 cache
        //  will prevent excess query when /logoOverride in turn calls getLogoOverride (again)
        return new ModelAndView(themeService.getMobileLogoOverride(username) != null ?
            "forward:mobileLogoOverride" : "forward:/" + String.format(STATIC_PATH, themeId) +
            "app-logo-mobile.png");
    }

    @GetMapping("/current")
    public String getUserTheme(@ModelAttribute("username") String username, @RequestParam(required = false) String siteId) {
        if ("".equals(siteId)) {
            siteId = null;
        }
        if (username == null)
            throw new IllegalArgumentException("Expecting username");
        return themeService.getThemeId(username, siteId);

    }
}

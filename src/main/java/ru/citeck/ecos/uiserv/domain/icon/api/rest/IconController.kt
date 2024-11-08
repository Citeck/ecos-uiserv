package ru.citeck.ecos.uiserv.domain.icon.api.rest

import org.springframework.http.CacheControl
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.citeck.ecos.uiserv.domain.icon.service.IconService
import ru.citeck.ecos.uiserv.domain.theme.api.rest.ThemeController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api")
class IconController(
    private val iconService: IconService,
    private val themeController: ThemeController
) {

    @GetMapping(path = ["/icon/{iconId}"], produces = ["text/css;charset=UTF-8"])
    fun getThemeStyle(
        @PathVariable("iconId") iconId : String
    ): HttpEntity<ByteArray> {

        val headers = HttpHeaders()
        headers.setCacheControl(
            CacheControl.maxAge(4, TimeUnit.HOURS)
                .mustRevalidate()
                .cachePublic()
        )

        val icon = iconService.findById(iconId).orElse(null)
            ?: return themeController.getNotFoundImageEntity(headers)

        val mimeType = icon.mimetype?.let { MediaType(it) } ?: run {
            val format = icon.config["format"].asText()
            when (format) {
                "jpg","jpeg" -> MediaType.IMAGE_JPEG
                "svg" -> MediaType.valueOf("image/svg+xml")
                "ico" -> MediaType.valueOf("image/x-icon")
                "png" -> MediaType.IMAGE_PNG
                else -> error("Unknown icon format: '$format'")
            }
        }

        headers.contentType = mimeType
        return HttpEntity<ByteArray>(icon.byteData, headers)
    }
}

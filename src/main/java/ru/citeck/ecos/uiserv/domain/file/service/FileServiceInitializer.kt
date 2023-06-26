package ru.citeck.ecos.uiserv.domain.file.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.uiserv.domain.file.service.FileService.FileMetadataExtractorInfo
import javax.annotation.PostConstruct

@Component
@Deprecated("FileService is a legacy service and should not be used")
class FileServiceInitializer(private val fileService: FileService) {

    private var extractors: Collection<FileMetadataExtractorInfo> = emptyList()

    @PostConstruct
    fun init() {
        fileService.setFileMetadataExtractors(extractors)
    }

    @Autowired(required = false)
    private fun setFileMetadataExtractors(extractors: Collection<FileMetadataExtractorInfo>) {
        this.extractors = extractors
    }
}

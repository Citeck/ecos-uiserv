package ru.citeck.ecos.uiserv.domain.file.service;

import java.util.Map;

@Deprecated
public class FileBundle {
    public final byte[] bytes;
    public final Map<String, byte[]> translations;

    public FileBundle(byte[] bytes, Map<String, byte[]> translations) {
        this.bytes = bytes;
        this.translations = translations;
    }
}

package ru.citeck.ecos.uiserv.web.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class JournalPrefsNotFoundException extends RuntimeException {

    public JournalPrefsNotFoundException(String id) {
        super("Journal preferences not found with id: " + id);
    }
}

package ru.citeck.ecos.uiserv.app.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthoritiesSupport {

    private final RecordsService recordsService;

    private PersonAtts queryPerson(String username) {
        return recordsService.getMeta(
            RecordRef.valueOf("alfresco/people@" + username),
            PersonAtts.class);
    }

    public List<String> queryUserAuthorities(String username) {
        return queryPerson(username).getAuthorities();
    }

    private static class PersonAtts {
        @AttName(".att(n:'authorities'){atts(n:'list')}")
        private List<String> authorities;

        public List<String> getAuthorities() {
            return authorities;
        }

        public void setAuthorities(List<String> authorities) {
            this.authorities = authorities;
        }
    }
}

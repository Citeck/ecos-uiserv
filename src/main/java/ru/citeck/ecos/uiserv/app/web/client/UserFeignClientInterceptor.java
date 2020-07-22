package ru.citeck.ecos.uiserv.app.web.client;

import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserFeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER = "Bearer";

    @Override
    public void apply(RequestTemplate template) {
        SecurityUtils.getCurrentUserJWT()
            .ifPresent(s -> {
                //AuthorizedFeignClient видимо по ошибке не подключается к микросервисам,
                //хотя в мануале именно его советуют применять, чтобы для некоторых сервисов
                //избежать перезаписи, для которой создан данный перехватчик
                //Поэтому придется в этот перехватчик вставить такой запрет перезаписи
                if (template.headers().containsKey(AUTHORIZATION_HEADER))
                    return;
                template.header(AUTHORIZATION_HEADER, String.format("%s %s", BEARER, s));
            });
    }
}

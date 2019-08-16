package ru.citeck.ecos.uiserv.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class UsernameModelProviderAdvice {
    private final ApplicationProperties properties;

    @ModelAttribute
    public void populateModel(@CookieValue(value = "alfUsername3", required = false) String fromCookie,
                              HttpServletRequest request, Model model) {

        String username = request.getParameter("username");
        if (StringUtils.isBlank(username)) {
            username = fromCookie;
        }
        if (StringUtils.isBlank(username)) {
            username = tryHeaders(properties.getTryHeaderForUsername(), request);
        }
        model.addAttribute("username", username);
    }

    private String tryHeaders(String checkHeader, HttpServletRequest request) {
        return Optional.ofNullable(checkHeader)
            .filter(x -> !x.equals(""))
            .map(request::getHeader)
            .orElse(null);
    }
}

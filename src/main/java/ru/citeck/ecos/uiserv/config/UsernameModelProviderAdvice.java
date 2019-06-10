package ru.citeck.ecos.uiserv.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class UsernameModelProviderAdvice {
    private final ApplicationProperties properties;

    @ModelAttribute
    public void populateModel(@CookieValue(value = "alfUsername3", required = false) String fromCookie,
                              HttpServletRequest request, Model model) {
        final String username = fromCookie != null ?
            fromCookie : tryHeaders(properties.getTryHeaderForUsername(), request);
        model.addAttribute("username", username);
    }

    private String tryHeaders(String checkHeader, HttpServletRequest request) {
        return Optional.ofNullable(checkHeader)
            .filter(x -> !x.equals(""))
            .map(request::getHeader)
            .orElse(null);
    }
}

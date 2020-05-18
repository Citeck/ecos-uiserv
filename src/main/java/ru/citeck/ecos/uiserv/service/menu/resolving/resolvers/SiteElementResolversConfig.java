package ru.citeck.ecos.uiserv.service.menu.resolving.resolvers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteElementResolversConfig {
    @Bean
    public SiteElementResolver siteCalendarResolver() {
        SiteElementResolver siteCalendarResolver = new SiteElementResolver();
        siteCalendarResolver.setId("SITE_CALENDAR");
        siteCalendarResolver.setElementTitleKey("menu.item.calendar");
        siteCalendarResolver.setPageLinkTemplate("site/%s/calendar");
        return siteCalendarResolver;
    }

    @Bean
    public SiteElementResolver siteDocLibResolver() {
        SiteElementResolver siteDocLibResolver = new SiteElementResolver();
        siteDocLibResolver.setId("SITE_DOCUMENT_LIBRARY");
        siteDocLibResolver.setElementTitleKey("menu.item.documentlibrary");
        siteDocLibResolver.setPageLinkTemplate("site/%s/documentlibrary");
        return siteDocLibResolver;
    }
}

package com.donation.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Configura el resolver de plantillas Thymeleaf para las vistas HTML servidas desde el classpath.
 */
@Configuration
public class ThymeleafTemplateConfig {
    
    /**
     * Define el resolver que busca plantillas en {@code templates/} con extensión {@code .html}.
     *
     * @return resolver de plantillas Thymeleaf
     */
    @Bean
    public ClassLoaderTemplateResolver templateResolver(){
        ClassLoaderTemplateResolver resolver=new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(1);
        resolver.setCheckExistence(true);

        return resolver;
    }
}

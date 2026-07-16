package com.stie.config;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MimeConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("pdf", "application/pdf");
        mappings.add("png", "image/png");
        mappings.add("jpg", "image/jpeg");
        mappings.add("jpeg", "image/jpeg");
        mappings.add("doc", "application/msword");
        mappings.add("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mappings.add("txt", "text/plain");
        factory.setMimeMappings(mappings);
    }
}

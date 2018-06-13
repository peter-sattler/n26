package com.sattler.n26.util;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

/**
 * JavaScript Object Notation (JSON) Error Message Converter
 * 
 * @author Peter Sattler
 * @implSpec This class is immutable and thread-safe
 */
public final class JsonError {

    private final String message;

    /**
     * Constructs a new JSON error converter
     * 
     * @param message The error message
     */
    public JsonError(String message) {
        this.message = message;
    }

    /**
     * Converts error message into a Spring {@code ModelAndView} object
     * 
     * @return The JSON error as an MVC model/view
     */
    public ModelAndView asModelAndView() {
        final MappingJackson2JsonView jsonView = new MappingJackson2JsonView();
        final Map<String, String> map = Collections.singletonMap("error", message);
        return new ModelAndView(jsonView, Collections.unmodifiableMap(map));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

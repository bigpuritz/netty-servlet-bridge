/*
 * Copyright 2013 by Maxim Kalina
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.javaforge.netty.servlet.bridge.config;

import net.javaforge.netty.servlet.bridge.impl.ConfigAdapter;
import net.javaforge.netty.servlet.bridge.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.regex.Pattern;

public abstract class HttpComponentConfigurationAdapter<COMP, CONFIG extends ConfigAdapter> {

    private static final Logger log = LoggerFactory
            .getLogger(HttpComponentConfigurationAdapter.class);

    private static final String DEFAULT_URL_PATTERN = "/*";

    private String[] sanitizedUrlPatterns;

    private Pattern[] regexPatterns;

    protected COMP component;

    protected CONFIG config;

    private boolean initialized = false;

    public HttpComponentConfigurationAdapter(
            Class<? extends COMP> componentClazz) {
        this(Utils.newInstance(componentClazz), DEFAULT_URL_PATTERN);
    }

    public HttpComponentConfigurationAdapter(
            Class<? extends COMP> servletClazz, String... urlPatterns) {
        this(Utils.newInstance(servletClazz), urlPatterns);
    }

    public HttpComponentConfigurationAdapter(COMP servlet) {
        this(servlet, DEFAULT_URL_PATTERN);
    }

    @SuppressWarnings("unchecked")
    public HttpComponentConfigurationAdapter(COMP component,
                                             String... urlPatterns) {
        if (urlPatterns == null || urlPatterns.length == 0)
            throw new IllegalStateException(
                    "No url patterns were assigned to http component: "
                            + component);

        this.regexPatterns = new Pattern[urlPatterns.length];
        this.sanitizedUrlPatterns = new String[urlPatterns.length];

        for (int i = 0; i < urlPatterns.length; i++) {
            String regex = urlPatterns[i].replaceAll("\\*", ".*");
            this.regexPatterns[i] = Pattern.compile(regex);
            this.sanitizedUrlPatterns[i] = urlPatterns[i].replaceAll("\\*", "");
            if (this.sanitizedUrlPatterns[i].endsWith("/"))
                this.sanitizedUrlPatterns[i] = this.sanitizedUrlPatterns[i]
                        .substring(0, this.sanitizedUrlPatterns[i].length() - 1);
        }

        this.component = component;
        this.config = newConfigInstance((Class<? extends COMP>) component
                .getClass());
    }

    protected abstract CONFIG newConfigInstance(
            Class<? extends COMP> componentClazz);

    public void init() {
        try {

            log.debug("Initializing http component: {}", this.component
                    .getClass());

            this.doInit();
            this.initialized = true;

        } catch (ServletException e) {

            this.initialized = false;
            log.error("Http component '" + this.component.getClass()
                    + "' was not initialized!", e);
        }
    }

    public void destroy() {
        try {

            log.debug("Destroying http component: {}", this.component
                    .getClass());

            this.doDestroy();
            this.initialized = false;

        } catch (ServletException e) {

            this.initialized = false;
            log.error("Http component '" + this.component.getClass()
                    + "' was not destroyed!", e);
        }
    }

    protected abstract void doInit() throws ServletException;

    protected abstract void doDestroy() throws ServletException;

    public boolean matchesUrlPattern(String uri) {
        return getMatchingUrlPattern(uri) != null;
    }

    public String getMatchingUrlPattern(String uri) {
        int indx = uri.indexOf('?');

        String path = indx != -1 ? uri.substring(0, indx) : uri.substring(0);
        if (!path.endsWith("/"))
            path += "/";

        for (int i = 0; i < regexPatterns.length; i++) {
            Pattern pattern = regexPatterns[i];
            if (pattern.matcher(path).matches()) {
                return sanitizedUrlPatterns[i];
            }
        }

        return null;

    }

    protected void addConfigInitParameter(String name, String value) {
        this.config.addInitParameter(name, value);
    }

    public COMP getHttpComponent() {
        return this.component;
    }

    public CONFIG getConfig() {
        return this.config;
    }

    public boolean isInitialized() {
        return initialized;
    }

}

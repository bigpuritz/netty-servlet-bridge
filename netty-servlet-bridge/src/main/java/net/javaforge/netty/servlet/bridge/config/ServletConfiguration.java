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

import net.javaforge.netty.servlet.bridge.impl.ServletConfigImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class ServletConfiguration extends
        HttpComponentConfigurationAdapter<HttpServlet, ServletConfigImpl> {

    public ServletConfiguration(Class<? extends HttpServlet> servletClazz,
                                String... urlPatterns) {
        super(servletClazz, urlPatterns);
    }

    public ServletConfiguration(Class<? extends HttpServlet> componentClazz) {
        super(componentClazz);
    }

    public ServletConfiguration(HttpServlet component, String... urlPatterns) {
        super(component, urlPatterns);
    }

    public ServletConfiguration(HttpServlet servlet) {
        super(servlet);
    }

    @Override
    protected void doInit() throws ServletException {
        this.component.init(this.config);
    }

    @Override
    protected void doDestroy() throws ServletException {
        this.component.destroy();
    }

    @Override
    protected ServletConfigImpl newConfigInstance(
            Class<? extends HttpServlet> componentClazz) {
        return new ServletConfigImpl(this.component.getClass().getName());
    }

    public ServletConfiguration addInitParameter(String name, String value) {
        super.addConfigInitParameter(name, value);
        return this;
    }
}

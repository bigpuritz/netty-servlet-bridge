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

import net.javaforge.netty.servlet.bridge.impl.FilterConfigImpl;

import javax.servlet.Filter;
import javax.servlet.ServletException;

public class FilterConfiguration extends
        HttpComponentConfigurationAdapter<Filter, FilterConfigImpl> {

    public FilterConfiguration(Class<? extends Filter> servletClazz,
                               String... urlPatterns) {
        super(servletClazz, urlPatterns);
    }

    public FilterConfiguration(Class<? extends Filter> componentClazz) {
        super(componentClazz);
    }

    public FilterConfiguration(Filter component, String... urlPatterns) {
        super(component, urlPatterns);
    }

    public FilterConfiguration(Filter servlet) {
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
    protected FilterConfigImpl newConfigInstance(
            Class<? extends Filter> componentClazz) {
        return new FilterConfigImpl(componentClazz.getName());
    }

    public FilterConfiguration addInitParameter(String name, String value) {
        super.addConfigInitParameter(name, value);
        return this;
    }

}

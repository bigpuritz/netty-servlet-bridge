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

package net.javaforge.netty.servlet.bridge.impl;

import io.netty.channel.group.ChannelGroup;
import net.javaforge.netty.servlet.bridge.config.FilterConfiguration;
import net.javaforge.netty.servlet.bridge.config.ServletConfiguration;
import net.javaforge.netty.servlet.bridge.config.ServletContextListenerConfiguration;
import net.javaforge.netty.servlet.bridge.config.WebappConfiguration;

import java.io.File;
import java.util.Map;

public class ServletBridgeWebapp {

    private static ServletBridgeWebapp instance;

    private WebappConfiguration webappConfig;

    private ChannelGroup sharedChannelGroup;

    public static ServletBridgeWebapp get() {

        if (instance == null)
            instance = new ServletBridgeWebapp();

        return instance;
    }

    private ServletBridgeWebapp() {
    }

    public void init(WebappConfiguration webapp, ChannelGroup sharedChannelGroup) {
        this.webappConfig = webapp;
        this.sharedChannelGroup = sharedChannelGroup;
        this.initServletContext();
        this.initContextListeners();
        this.initFilters();
        this.initServlets();
    }

    public void destroy() {
        this.destroyServlets();
        this.destroyFilters();
        this.destroyContextListeners();
    }

    private void initContextListeners() {
        if (webappConfig.getServletContextListenerConfigurations() != null) {
            for (ServletContextListenerConfiguration ctx : webappConfig
                    .getServletContextListenerConfigurations()) {
                ctx.init();
            }
        }
    }

    private void destroyContextListeners() {
        if (webappConfig.getServletContextListenerConfigurations() != null) {
            for (ServletContextListenerConfiguration ctx : webappConfig
                    .getServletContextListenerConfigurations()) {
                ctx.destroy();
            }
        }
    }

    private void destroyServlets() {
        if (webappConfig.getServletConfigurations() != null) {
            for (ServletConfiguration servlet : webappConfig
                    .getServletConfigurations()) {
                servlet.destroy();
            }
        }
    }

    private void destroyFilters() {
        if (webappConfig.getFilterConfigurations() != null) {
            for (FilterConfiguration filter : webappConfig
                    .getFilterConfigurations()) {
                filter.destroy();
            }
        }
    }

    protected void initServletContext() {
        ServletContextImpl ctx = ServletContextImpl.get();
        ctx.setServletContextName(this.webappConfig.getName());
        if (webappConfig.getContextParameters() != null) {
            for (Map.Entry<String, String> entry : webappConfig
                    .getContextParameters().entrySet()) {
                ctx.addInitParameter(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void initFilters() {
        if (webappConfig.getFilterConfigurations() != null) {
            for (FilterConfiguration filter : webappConfig
                    .getFilterConfigurations()) {
                filter.init();
            }
        }
    }

    protected void initServlets() {
        if (webappConfig.hasServletConfigurations()) {
            for (ServletConfiguration servlet : webappConfig
                    .getServletConfigurations()) {
                servlet.init();
            }
        }
    }

    public FilterChainImpl initializeChain(String uri) {
        ServletConfiguration servletConfiguration = this.findServlet(uri);
        FilterChainImpl chain = new FilterChainImpl(servletConfiguration);

        if (this.webappConfig.hasFilterConfigurations()) {
            for (FilterConfiguration s : this.webappConfig
                    .getFilterConfigurations()) {
                if (s.matchesUrlPattern(uri))
                    chain.addFilterConfiguration(s);
            }
        }

        return chain;
    }

    private ServletConfiguration findServlet(String uri) {

        if (!this.webappConfig.hasServletConfigurations()) {
            return null;
        }

        for (ServletConfiguration s : this.webappConfig
                .getServletConfigurations()) {
            if (s.matchesUrlPattern(uri))
                return s;
        }

        return null;
    }

    public File getStaticResourcesFolder() {
        return this.webappConfig.getStaticResourcesFolder();
    }

    public WebappConfiguration getWebappConfig() {
        return webappConfig;
    }

    public ChannelGroup getSharedChannelGroup() {
        return sharedChannelGroup;
    }
}

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

import net.javaforge.netty.servlet.bridge.impl.ServletContextImpl;
import net.javaforge.netty.servlet.bridge.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServletContextListenerConfiguration {

    private static final Logger log = LoggerFactory
            .getLogger(ServletContextListenerConfiguration.class);

    private ServletContextListener listener;

    private boolean initialized = false;

    public ServletContextListenerConfiguration(
            Class<? extends ServletContextListener> clazz) {
        this(Utils.newInstance(clazz));
    }

    public ServletContextListenerConfiguration(ServletContextListener listener) {
        this.listener = listener;
    }

    public ServletContextListener getListener() {
        return listener;
    }

    public void init() {
        try {

            log.debug("Initializing listener: {}", this.listener.getClass());

            this.listener.contextInitialized(new ServletContextEvent(
                    ServletContextImpl.get()));
            this.initialized = true;

        } catch (Exception e) {

            this.initialized = false;
            log.error("Listener '" + this.listener.getClass()
                    + "' was not initialized!", e);
        }
    }

    public void destroy() {
        try {

            log.debug("Destroying listener: {}", this.listener.getClass());

            this.listener.contextDestroyed(new ServletContextEvent(
                    ServletContextImpl.get()));
            this.initialized = false;

        } catch (Exception e) {

            this.initialized = false;
            log.error("Listener '" + this.listener.getClass()
                    + "' was not destroyed!", e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}

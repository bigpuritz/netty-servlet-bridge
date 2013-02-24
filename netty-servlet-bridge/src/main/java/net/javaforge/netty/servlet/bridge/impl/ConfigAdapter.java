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

import net.javaforge.netty.servlet.bridge.util.Utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfigAdapter {

    private String ownerName;

    private Map<String, String> initParameters;

    public ConfigAdapter(String ownerName) {
        this.ownerName = ownerName;
    }

    public void addInitParameter(String name, String value) {
        if (this.initParameters == null)
            this.initParameters = new HashMap<String, String>();

        this.initParameters.put(name, value);
    }

    public String getInitParameter(String name) {
        if (this.initParameters == null)
            return null;

        return this.initParameters.get(name);
    }

    @SuppressWarnings("unchecked")
    public Enumeration getInitParameterNames() {
        return Utils.enumerationFromKeys(this.initParameters);
    }

    String getOwnerName() {
        return ownerName;
    }

}

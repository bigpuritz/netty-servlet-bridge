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

import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;

import javax.servlet.ServletInputStream;
import java.io.IOException;

public class ServletInputStreamImpl extends ServletInputStream {

    private HttpRequest request;

    private ChannelBufferInputStream in;

    public ServletInputStreamImpl(HttpRequest request) {
        this.request = request;
        this.in = new ChannelBufferInputStream(this.request.getContent());
    }

    @Override
    public int read() throws IOException {
        return this.in.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return this.in.read(buf);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        return this.in.read(buf,offset,len);
    }

}

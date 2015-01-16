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

package net.javaforge.netty.servlet.bridge.interceptor;

import net.javaforge.netty.servlet.bridge.HttpSessionThreadLocal;
import net.javaforge.netty.servlet.bridge.ServletBridgeInterceptor;
import net.javaforge.netty.servlet.bridge.impl.HttpSessionImpl;
import net.javaforge.netty.servlet.bridge.session.ServletBridgeHttpSessionStore;
import net.javaforge.netty.servlet.bridge.util.Utils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.Collection;

import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

public class HttpSessionInterceptor implements ServletBridgeInterceptor {

    private boolean sessionRequestedByCookie = false;

    public HttpSessionInterceptor(ServletBridgeHttpSessionStore sessionStore) {
        HttpSessionThreadLocal.setSessionStore(sessionStore);
    }

    @Override
    public void onRequestReceived(ChannelHandlerContext ctx, HttpRequest request) {

        HttpSessionThreadLocal.unset();

        Collection<Cookie> cookies = Utils.getCookies(
                HttpSessionImpl.SESSION_ID_KEY, request);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String jsessionId = cookie.getValue();
                HttpSessionImpl s = HttpSessionThreadLocal.getSessionStore()
                        .findSession(jsessionId);
                if (s != null) {
                    HttpSessionThreadLocal.set(s);
                    this.sessionRequestedByCookie = true;
                    break;
                }
            }
        }
    }

    @Override
    public void onRequestSuccessed(ChannelHandlerContext ctx, HttpRequest request,
                                   HttpResponse response) {

        HttpSessionImpl s = HttpSessionThreadLocal.get();
        if (s != null && !this.sessionRequestedByCookie) {
            HttpHeaders.addHeader(response, SET_COOKIE, ServerCookieEncoder.encode(HttpSessionImpl.SESSION_ID_KEY, s.getId()));
        }

    }

    @Override
    public void onRequestFailed(ChannelHandlerContext ctx, Throwable e,
                                HttpResponse response) {
        this.sessionRequestedByCookie = false;
        HttpSessionThreadLocal.unset();
    }

}

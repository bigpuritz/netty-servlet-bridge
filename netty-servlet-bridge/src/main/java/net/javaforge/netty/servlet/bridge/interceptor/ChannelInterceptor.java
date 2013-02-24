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

import net.javaforge.netty.servlet.bridge.ChannelThreadLocal;
import net.javaforge.netty.servlet.bridge.ServletBridgeInterceptor;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class ChannelInterceptor implements ServletBridgeInterceptor {

    @Override
    public void onRequestFailed(ChannelHandlerContext ctx, ExceptionEvent e,
                                HttpResponse response) {
        ChannelThreadLocal.unset();
    }

    @Override
    public void onRequestReceived(ChannelHandlerContext ctx, MessageEvent e) {
        ChannelThreadLocal.set(e.getChannel());
    }

    @Override
    public void onRequestSuccessed(ChannelHandlerContext ctx, MessageEvent e,
                                   HttpResponse response) {
        ChannelThreadLocal.unset();
    }

}

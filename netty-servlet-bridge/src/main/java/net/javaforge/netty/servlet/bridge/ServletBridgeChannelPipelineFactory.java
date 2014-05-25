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

package net.javaforge.netty.servlet.bridge;

import net.javaforge.netty.servlet.bridge.config.WebappConfiguration;
import net.javaforge.netty.servlet.bridge.impl.ServletBridgeWebapp;
import net.javaforge.netty.servlet.bridge.interceptor.ChannelInterceptor;
import net.javaforge.netty.servlet.bridge.interceptor.HttpSessionInterceptor;
import net.javaforge.netty.servlet.bridge.session.DefaultServletBridgeHttpSessionStore;
import net.javaforge.netty.servlet.bridge.session.ServletBridgeHttpSessionStore;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import static org.jboss.netty.channel.Channels.pipeline;

public class ServletBridgeChannelPipelineFactory implements
        ChannelPipelineFactory {

    private ChannelGroup allChannels = new DefaultChannelGroup();

    private HttpSessionWatchdog watchdog;

    private final ChannelHandler idleStateHandler;

    private Timer timer;

    public ServletBridgeChannelPipelineFactory(WebappConfiguration config) {

        this.timer = new HashedWheelTimer();
        this.idleStateHandler = new IdleStateHandler(this.timer, 60, 30, 0); // timer
        // must
        // be
        // shared.

        ServletBridgeWebapp webapp = ServletBridgeWebapp.get();
        webapp.init(config, allChannels);

        new Thread(this.watchdog = new HttpSessionWatchdog()).start();
    }

    public void shutdown() {
        this.watchdog.stopWatching();
        ServletBridgeWebapp.get().destroy();
        this.timer.stop();
        this.allChannels.close().awaitUninterruptibly();
    }

    
    @Override
    public final ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = this.getDefaulHttpChannelPipeline();
        pipeline.addLast("handler", this.getServletBridgeHandler());
        return pipeline;
    }

    protected ServletBridgeHttpSessionStore getHttpSessionStore() {
        return new DefaultServletBridgeHttpSessionStore();
    }

    protected ServletBridgeHandler getServletBridgeHandler() {

        ServletBridgeHandler bridge = new ServletBridgeHandler();
        bridge.addInterceptor(new ChannelInterceptor());
        bridge
                .addInterceptor(new HttpSessionInterceptor(
                        getHttpSessionStore()));
        return bridge;
    }

    protected ChannelPipeline getDefaulHttpChannelPipeline() {

        // Create a default pipeline implementation.
        ChannelPipeline pipeline = pipeline();

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("encoder", new HttpResponseEncoder());

        // Remove the following line if you don't want automatic content
        // compression.
        pipeline.addLast("deflater", new HttpContentCompressor());
        pipeline.addLast("idle", this.idleStateHandler);

        return pipeline;
    }
    
    public ChannelGroup getAllChannels() {
      	return allChannels;
    }
        
    public void setAllChannels(ChannelGroup allChannels) {
      	this.allChannels = allChannels;
    }

    private class HttpSessionWatchdog implements Runnable {

        private boolean shouldStopWatching = false;

        @Override
        public void run() {

            while (!shouldStopWatching) {

                try {
                    ServletBridgeHttpSessionStore store = getHttpSessionStore();
                    if (store != null) {
                        store.destroyInactiveSessions();
                    }
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    return;
                }

            }

        }

        public void stopWatching() {
            this.shouldStopWatching = true;
        }

    }
}

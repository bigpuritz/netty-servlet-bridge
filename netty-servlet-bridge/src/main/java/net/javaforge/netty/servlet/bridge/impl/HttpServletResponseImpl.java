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

import io.netty.handler.codec.http.*;
import net.javaforge.netty.servlet.bridge.ServletBridgeRuntimeException;
import io.netty.handler.codec.http.HttpHeaders.Names;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;
import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;

public class HttpServletResponseImpl implements HttpServletResponse {
    private HttpResponse originalResponse;
    private ServletOutputStreamImpl outputStream;
    private PrintWriterImpl writer;
    private boolean responseCommited = false;
    private Locale locale = null;

    public HttpServletResponseImpl(FullHttpResponse response) {
        this.originalResponse = response;
        this.outputStream = new ServletOutputStreamImpl(response);
        this.writer = new PrintWriterImpl(this.outputStream);
    }

    public HttpResponse getOriginalResponse() {
        return originalResponse;
    }

    @Override
    public void addCookie(Cookie cookie) {
        String result = ServerCookieEncoder.encode(new io.netty.handler.codec.http.DefaultCookie(cookie.getName(), cookie.getValue()));
        HttpHeaders.addHeader(this.originalResponse, SET_COOKIE, result);
    }

    @Override
    public void addDateHeader(String name, long date) {
        HttpHeaders.addHeader(this.originalResponse, name, date);
    }

    @Override
    public void addHeader(String name, String value) {
        HttpHeaders.addHeader(this.originalResponse, name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        HttpHeaders.addIntHeader(this.originalResponse, name, value);
    }

    @Override
    public boolean containsHeader(String name) {
        return this.originalResponse.headers().contains(name);
    }

    @Override
    public void sendError(int sc) throws IOException {
        this.originalResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        //Fix the following exception
        /*
        java.lang.IllegalArgumentException: reasonPhrase contains one of the following prohibited characters: \r\n: FAILED - Cannot find View Map for null.

            at io.netty.handler.codec.http.HttpResponseStatus.<init>(HttpResponseStatus.java:514) ~[netty-all-4.1.0.Beta3.jar:4.1.0.Beta3]
        at io.netty.handler.codec.http.HttpResponseStatus.<init>(HttpResponseStatus.java:496) ~[netty-all-4.1.0.Beta3.jar:4.1.0.Beta3]
        */
        if (msg != null) {
            msg = msg.replace('\r', ' ');
            msg = msg.replace('\n', ' ');
        }
        this.originalResponse.setStatus(new HttpResponseStatus(sc, msg));
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        setStatus(SC_FOUND);
        setHeader(LOCATION, location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        HttpHeaders.setHeader(this.originalResponse, name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        HttpHeaders.setHeader(this.originalResponse, name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        HttpHeaders.setIntHeader(this.originalResponse, name, value);

    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return this.outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return this.writer;
    }

    @Override
    public void setStatus(int sc) {
        this.originalResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    public void setStatus(int sc, String sm) {
        this.originalResponse.setStatus(new HttpResponseStatus(sc, sm));
    }

    @Override
    public String getContentType() {
        return HttpHeaders.getHeader(this.originalResponse,
                HttpHeaders.Names.CONTENT_TYPE);
    }

    @Override
    public void setContentType(String type) {
        HttpHeaders.setHeader(this.originalResponse,
                HttpHeaders.Names.CONTENT_TYPE, type);
    }

    @Override
    public void setContentLength(int len) {
        HttpHeaders.setContentLength(this.originalResponse, len);
    }

    @Override
    public boolean isCommitted() {
        return this.responseCommited;
    }

    @Override
    public void reset() {
        if (isCommitted())
            throw new IllegalStateException("Response already commited!");

        this.originalResponse.headers().clear();
        this.resetBuffer();
    }

    @Override
    public void resetBuffer() {
        if (isCommitted())
            throw new IllegalStateException("Response already commited!");

        this.outputStream.resetBuffer();
    }

    @Override
    public void flushBuffer() throws IOException {
        this.getWriter().flush();
        this.responseCommited = true;
    }

    @Override
    public int getBufferSize() {
        return this.outputStream.getBufferSize();
    }

    @Override
    public void setBufferSize(int size) {
        // we using always dynamic buffer for now
    }

    @Override
    public String encodeRedirectURL(String url) {
        return this.encodeURL(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return this.encodeURL(url);
    }

    @Override
    public String encodeURL(String url) {
        try {
            return URLEncoder.encode(url, getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            throw new ServletBridgeRuntimeException("Error encoding url!", e);
        }
    }

    @Override
    public String encodeUrl(String url) {
        return this.encodeRedirectURL(url);
    }

    @Override
    public String getCharacterEncoding() {
        return HttpHeaders.getHeader(this.originalResponse,
                Names.CONTENT_ENCODING);
    }

    @Override
    public void setCharacterEncoding(String charset) {
        HttpHeaders.setHeader(this.originalResponse,
                Names.CONTENT_ENCODING, charset);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void setLocale(Locale loc) {
        this.locale = loc;
    }
}
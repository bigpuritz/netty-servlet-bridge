package net.javaforge.netty.servlet.bridge.impl;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Simple RequestDispatcher Implementation
 *
 * Created by xshao on 12/22/14.
 */
public class RequestDispatcherImpl implements RequestDispatcher {

    /**
     * The servlet name for a named dispatcher.
     */
    private String name = null;

    /**
     * The servlet path for this RequestDispatcher.
     */
    private String servletPath = null;


    private HttpServlet httpServlet;


    public RequestDispatcherImpl(String servletName, String servletPath, HttpServlet servlet) {
        this.name = servletName;
        this.servletPath = servletPath;
        this.httpServlet = servlet;
    }


    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        if (httpServlet != null) {
            //TODO Wrap
            httpServlet.service(servletRequest, servletResponse);
        }
        else {
            ((HttpServletResponse)servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        if (httpServlet != null) {
            //TODO Wrap
            httpServlet.service(servletRequest, servletResponse);
        }
        else {
            ((HttpServletResponse)servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}

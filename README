This project provides a Servlet API implementation for Netty.IO framework (http://netty.io/).

Netty Servlet Bridge allows integration of existing Servlet API based web-applications
into the Netty-backed infrastructure.

Simple usage example:


```java
// Configure netty server.
final ServerBootstrap bootstrap = new ServerBootstrap(
    new NioServerSocketChannelFactory(
        Executors.newCachedThreadPool(), Executors.newCachedThreadPool()
    )
);

// configure web-app
WebappConfiguration webapp = new WebappConfiguration()
    .addContextParameter("name1", "value1")
    .addContextParameter("name2", "value2")
    .addServletConfigurations(
        new ServletConfiguration(MyHttpServlet.class, "/servlet1"),
        new ServletConfiguration(MyAnotherHttpServlet.class, "/servlet2"),
            .addInitParameter("initpram1", "value1")
            .addInitParameter("initpram2", "value2")

    )
    .addFilterConfigurations (
        new FilterConfiguration(MyServletFilter.class, "/*"),
    );


// Set up the event pipeline factory.
bootstrap.setPipelineFactory(new ServletBridgeChannelPipelineFactory(webapp));

// Bind and start to accept incoming connections.
final Channel serverChannel = bootstrap.bind(new InetSocketAddress(8080));

```

Please consult example projects demonstrating the library usage.

Jersey Integration Example: https://github.com/bigpuritz/netty-servlet-bridge/tree/master/jersey-netty-example
Vaadin Integration Example: https://github.com/bigpuritz/netty-servlet-bridge/tree/master/vaadin-netty-example

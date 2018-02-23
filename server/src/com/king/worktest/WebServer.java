package com.king.worktest;

import com.acme.KingServlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.tomcat.util.scan.StandardJarScanner;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.Configuration;

import java.io.File;


public class WebServer {
	public static class JspStarter extends AbstractLifeCycle implements ServletContextHandler.ServletContainerInitializerCaller
	{
			JettyJasperInitializer sci;
			ServletContextHandler context;

			public JspStarter (ServletContextHandler context)
			{
					this.sci = new JettyJasperInitializer();
					this.context = context;
					this.context.setAttribute("org.apache.tomcat.JarScanner", new StandardJarScanner());
			}

			@Override
			protected void doStart() throws Exception
			{
					ClassLoader old = Thread.currentThread().getContextClassLoader();
					Thread.currentThread().setContextClassLoader(context.getClassLoader());
					try
					{
							sci.onStartup(null, context.getServletContext());
							super.doStart();
					}
					finally
					{
							Thread.currentThread().setContextClassLoader(old);
					}
			}
	}

	private final Logger LOG = LoggerFactory.getLogger(getClass());
	private final Server server;
	private static final String WEBROOT_INDEX = "/web/";

	public WebServer(int port) throws Exception {
		server = new Server(port);
		createWebAppContext(server);
	}

	public void startAndJoin() throws Exception {
		server.start();
		server.join();
	}

	/**
	 * Setup JSP Support for ServletContextHandlers.
	 * <p>
	 *   NOTE: This is not required or appropriate if using a WebAppContext.
	 * </p>
	 *
	 * @param servletContextHandler the ServletContextHandler to configure
	 * @throws IOException if unable to configure
	 */
	private void enableEmbeddedJspSupport(ServletContextHandler servletContextHandler) throws IOException
	{
			// Establish Scratch directory for the servlet context (used by JSP compilation)
			File tempDir = new File(System.getProperty("java.io.tmpdir"));
			File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

			if (!scratchDir.exists())
			{
					if (!scratchDir.mkdirs())
					{
							throw new IOException("Unable to create scratch directory: " + scratchDir);
					}
			}
			servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);

			// Set Classloader of Context to be sane (needed for JSTL)
			// JSP requires a non-System classloader, this simply wraps the
			// embedded System classloader in a way that makes it suitable
			// for JSP to use
			ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
			servletContextHandler.setClassLoader(jspClassLoader);

			// Manually call JettyJasperInitializer on context startup
			servletContextHandler.addBean(new JspStarter(servletContextHandler));

			// Create / Register JSP Servlet (must be named "jsp" per spec)
			ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
			holderJsp.setInitOrder(0);
			holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
			holderJsp.setInitParameter("fork", "false");
			holderJsp.setInitParameter("xpoweredBy", "false");
			holderJsp.setInitParameter("compilerTargetVM", "1.8");
			holderJsp.setInitParameter("compilerSourceVM", "1.8");
			holderJsp.setInitParameter("keepgenerated", "true");
			servletContextHandler.addServlet(holderJsp, "*.jsp");
	}

	private URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException
	{
			URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
			if (indexUri == null)
			{
					throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
			}
			// Points to wherever /webroot/ (the resource) is
			return indexUri.toURI();
	}

	private void createWebAppContext(Server server) throws Exception
	{
		// Define ServerConnector
		// Define ServerConnector
		ServerConnector connector = new ServerConnector(server);
		//connector.setPort(port);
		server.addConnector(connector);

		// Add annotation scanning (for WebAppContexts)
		Configuration.ClassList classlist = Configuration.ClassList
						.setServerDefault( server );
		classlist.addBefore(
						"org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
						"org.eclipse.jetty.annotations.AnnotationConfiguration" );

		// Base URI for servlet context
		URI baseUri = getWebRootResourceUri();
		LOG.info("Base URI: " + baseUri);

		// Create Servlet context
		ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		servletContextHandler.setContextPath("/");
		servletContextHandler.setResourceBase(baseUri.toASCIIString());

		// Since this is a ServletContextHandler we must manually configure JSP support.
		enableEmbeddedJspSupport(servletContextHandler);

		// Default Servlet (always last, always named "default")
		ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
		holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString());
		holderDefault.setInitParameter("dirAllowed", "true");
		servletContextHandler.addServlet(holderDefault, "/index.jsp");
		server.setHandler(servletContextHandler);
	}
}

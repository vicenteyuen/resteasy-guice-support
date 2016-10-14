/**
 * 
 */
package org.vsg.resteasy.plugins.server.servlet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.plugins.servlet.i18n.Messages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

/**
 * @author Vicente Yuen
 *
 */
public class GuiceBootstrap extends ResteasyBootstrap {
	
	
	private static Logger logger = LoggerFactory.getLogger(GuiceBootstrap.class);

	private List<? extends Module> modules;
	@Inject
	private Injector parentInjector = null;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// --- change order ---
		super.contextInitialized(event);
		ServletContext context = event.getServletContext();

		this.deployment.setAsyncJobServiceEnabled(true);

		List<? extends Module> modules = getModules(context);
		Stage stage = getStage(context);
		Injector injector;

		if (parentInjector != null) {
			injector = parentInjector.createChildInjector(modules);
		} else {
			if (stage == null) {
				injector = Guice.createInjector(modules);
			} else {
				injector = Guice.createInjector(stage, modules);
			}
		}
		// --- bindig modules
		this.modules = modules;

		applicationClzz = getApplications(context);

		// --- binding injector --
		withInjector(injector);

		// ---- create application handle ---

		/*
		 * 
		 * 
		 * Registry registry = (Registry)
		 * context.getAttribute(Registry.class.getName());
		 * 
		 * 
		 * final ResteasyProviderFactory providerFactory =
		 * (ResteasyProviderFactory)
		 * context.getAttribute(ResteasyProviderFactory.class.getName()); final
		 * ModuleProcessor processor = new ModuleProcessor(registry,
		 * providerFactory); final List<? extends Module> modules =
		 * getModules(context); final Stage stage = getStage(context); Injector
		 * injector;
		 * 
		 * if (parentInjector != null) { injector =
		 * parentInjector.createChildInjector(modules); } else { if (stage ==
		 * null) { injector = Guice.createInjector(modules); } else { injector =
		 * Guice.createInjector(stage, modules); } } withInjector(injector);
		 * processor.processInjector(injector);
		 * 
		 * //load parent injectors while (injector.getParent() != null) {
		 * injector = injector.getParent(); processor.processInjector(injector);
		 * } this.modules = modules;
		 * triggerAnnotatedMethods(PostConstruct.class);
		 * 
		 * 
		 * // --- bind application --- context.setAttribute("guice.injector",
		 * injector);
		 */
		context.setAttribute("guice.injector", injector);
	}

	private Class<? extends Application>[] applicationClzz;

	/**
	 * Override this method to interact with the {@link Injector} after it has
	 * been created. The default is no-op.
	 *
	 * @param injector
	 */
	protected void withInjector(Injector injector) {

		// --- binding application context ----
		for (Class<? extends Application> appClz : applicationClzz) {
			ApplicationPath appPath = appClz.getAnnotation(ApplicationPath.class);
			String path = "/";
			if (appPath != null) {
				path = appPath.value();
			}

			try {
				Constructor<? extends Application> constructor = appClz.getConstructor(Injector.class);

				Application application = constructor.newInstance(injector);

				// --- set deployment app ---
				this.deployment.setApplication(application);

			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		/*
		 * for (Application application : applications) { ApplicationPath
		 * appPath =
		 * application.getClass().getAnnotation(ApplicationPath.class); String
		 * path = "/"; if (appPath != null) { path = appPath.value(); }
		 * 
		 * // --- bind application ---
		 * 
		 * 
		 * this.deployment.setApplication(application);
		 * 
		 * }
		 */

	}

	private Class<? extends Application>[] getApplications(ServletContext context) {
		String applicationsStr = context.getInitParameter("cms.rs.Applications");

		if (applicationsStr != null) {
			String[] appsStrings = applicationsStr.trim().split(",");
			Collection<Class<? extends Application>> applications = new Vector<Class<? extends Application>>();
			int i = 0;
			for (String appString : appsStrings) {
				try {
					LogMessages.LOGGER.info("Found application \"" + appString + "\". ");
					Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(appString.trim());
					// Application app = (Application) clazz.newInstance();
					// apps[i++] = app;
					applications.add((Class<? extends Application>) clazz);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}

			}
			return applications.toArray(new Class[0]);

		} else {
			return null;
		}
	}

	/**
	 * Override this method to set the Stage. By default it is taken from
	 * resteasy.guice.stage context param.
	 *
	 * @param context
	 * @return Guice Stage
	 */
	protected Stage getStage(ServletContext context) {
		final String stageAsString = context.getInitParameter("resteasy.guice.stage");
		if (stageAsString == null) {
			return null;
		}
		try {
			return Stage.valueOf(stageAsString.trim());
		} catch (IllegalArgumentException e) {
			
			String stageMessage = "Your define stage is not support.";
			
			throw new RuntimeException(stageMessage + " , " + stageAsString);
		}
	}

	/**
	 * Override this method to instantiate your {@link Module}s yourself.
	 *
	 * @param context
	 * @return
	 */
	protected List<? extends Module> getModules(final ServletContext context) {
		final List<Module> result = new ArrayList<Module>();
		final String modulesString = context.getInitParameter("resteasy.guice.modules");
		if (modulesString != null) {
			final String[] moduleStrings = modulesString.trim().split(",");
			for (final String moduleString : moduleStrings) {
				try {
					LogMessages.LOGGER.info(moduleString);
					final Class<?> clazz = Thread.currentThread().getContextClassLoader()
							.loadClass(moduleString.trim());
					final Module module = (Module) clazz.newInstance();
					result.add(module);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				}
			}

		}
		return result;
	}

	@Override
	public void contextDestroyed(final ServletContextEvent event) {
		triggerAnnotatedMethods(PreDestroy.class);
	}

	private void triggerAnnotatedMethods(final Class<? extends Annotation> annotationClass) {
		for (Module module : this.modules) {
			final Method[] methods = module.getClass().getMethods();
			for (Method method : methods) {
				if (method.isAnnotationPresent(annotationClass)) {
					if (method.getParameterTypes().length > 0) {
						logger.warn("Could not execute method : " + module.getClass().getSimpleName() + " , " + annotationClass.getSimpleName() + " , " + method.getName());
						continue;
					}
					try {
						method.invoke(module);
					} catch (InvocationTargetException ex) {
						LogMessages.LOGGER.warn("Could not found method for annotation class " + annotationClass.getSimpleName(), ex);
					} catch (IllegalAccessException ex) {
						logger.warn("Running annotation method : " + annotationClass.getSimpleName() + " , " + ex.getMessage());

					}
				}
			}
		}
	}

}

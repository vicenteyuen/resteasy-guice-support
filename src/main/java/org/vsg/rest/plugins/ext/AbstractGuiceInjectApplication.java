/**
 * 
 */
package org.vsg.rest.plugins.ext;

import javax.ws.rs.core.Application;

import com.google.inject.Injector;

/**
 * @author ruanweibiao
 *
 */
public abstract class AbstractGuiceInjectApplication extends Application {

	private Injector injector;	
	
	
	public AbstractGuiceInjectApplication() {
		
	}
	
	public AbstractGuiceInjectApplication(Injector injector) {
		this.injector = injector;
		
		// --- get annotation package ---
		scanPackage();
	}
	
	public Injector getInjector() {
		return this.injector;
	}
	
	
	private void scanPackage() {
		
	}
	
}

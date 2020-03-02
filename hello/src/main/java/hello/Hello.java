package hello;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component
public class Hello {

	@Activate
	public void start(BundleContext context) throws Exception {
		System.err.println("Hello World");
	}

	@Deactivate
	public void stop(BundleContext context) throws Exception {
		System.err.println("Goodbye World");
	}

	
}

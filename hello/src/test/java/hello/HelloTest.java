package hello;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import hello.Hello;

public class HelloTest {
	@Test
	public void simple() {
		Hello impl = new Hello();
		assertNotNull(impl);
	}

}

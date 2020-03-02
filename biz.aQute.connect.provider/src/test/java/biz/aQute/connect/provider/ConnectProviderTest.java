package biz.aQute.connect.provider;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

import aQute.lib.io.IO;
import biz.aQute.connect.plugin.Flatter;

public class ConnectProviderTest {
	
	@Test
	public void testConnectBndAdapterInPlainJava() throws Exception {
		File in = IO.getFile("../biz.aQute.connect.playground/target/playground.bndrun.jar");
		File out = IO.getFile("target/output.jar");
		IO.delete(out);
		Flatter.flatten(in, out);

		try (URLClassLoader cl = new URLClassLoader(new URL[] {out.toURI().toURL()},null)) {

			Class<?> loadClass = cl.loadClass("aQute.launcher.Launcher");
			Method method = loadClass.getMethod("main", String[].class);
			method.invoke(null, (Object)new String[0]);
		}
	}

}

package biz.aQute.connect.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Manifest;

import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

public class Flatter {
	public static void main(String[] args) throws Exception {

		File in = IO.getFile("../biz.aQute.connect.playground/target/playground.bndrun.jar");
		//File in = IO.getFile("application.bndrun.jar");
		File out = IO.getFile("target/output.jar");
		IO.delete(out);
		flatten(in, out);

	}

	public static void flatten(File in, File out) throws Exception {
		List<String> index = new ArrayList<>();
		List<Jar> subs = new ArrayList<>();

		try (Jar jout = new Jar(out.getName())) {

			try (Jar jin = new Jar(in)) {

				Manifest m = jin.getManifest();
				String runpathS = m.getMainAttributes().getValue("Embedded-Runpath");
				List<String> runpath = Strings.splitQuoted(runpathS);

				for (Entry<String, Resource> e : new HashMap<>(jin.getResources()).entrySet()) {
					if (e.getKey().endsWith(".jar")) {

						if (runpath.contains(e.getKey()))
							continue;

						System.out.println("Resource " + e.getKey());
						int n = index.size();
						index.add(e.getKey());

						flatten(jin.getResource(e.getKey()), jout, "BUNDLES/bundle-" + n + "/", subs);

					} else {
						if (!e.getKey().startsWith("OSGI-OPT/"))
							jout.putResource(e.getKey(), e.getValue());
						else
							System.out.println("Skipping " + e.getKey());
					}
				}

				Collections.reverse(runpath);
				for (String path : runpath) {
					flatten(jin.getResource(path), jout, "", subs);
				}

				String bs = Strings.join(index);
				jout.putResource("BUNDLES/index", new EmbeddedResource(bs, 0));

				jout.write(out);
				subs.forEach(Jar::close);
			}
		}
	}

	private static void flatten(Resource resource, Jar jout, String resourcePrefix, List<Jar> toClose)
			throws IOException, Exception {
		Jar jar = new Jar(".", resource.openInputStream());
		toClose.add(jar);
		List<String> bundleEntries = new ArrayList<>();

		for (Entry<String, Resource> e : jar.getResources().entrySet()) {

			bundleEntries.add(e.getKey());

			if (e.getKey().startsWith("OSGI-OPT/")) {
				continue;
			} else {
				Resource previous = jout.getResource(e.getKey());
				boolean isDuplicate = previous != null;
				if ( isDuplicate && isEqual(previous,e.getValue()))
						continue;
				
				String newPath = e.getKey();
				boolean isOsgiAware = e.getKey().startsWith("OSGI-") || e.getKey().equals("META-INF/MANIFEST.MF");
				boolean isClass =e.getKey().endsWith(".class");
				
				if ( isClass && isDuplicate) {
						System.err.println("Dupl != class " + e.getKey() + " <- " + jar.getBsn() + " skipping!");
						continue;
				}
				
				if (isOsgiAware || isDuplicate) {
					newPath = resourcePrefix + e.getKey();
				}
				jout.putResource(newPath, e.getValue());
			}
		}

		String index = Strings.join(bundleEntries);
		jout.putResource(resourcePrefix + "index", new EmbeddedResource(index, 0));
	}

	private static boolean isEqual(Resource previous, Resource value) throws IOException, Exception {
		if ( previous == null || value == null)
			return false;
		
		if ( previous == value)
			return true;
		
		byte[] a = IO.read( previous.openInputStream());
		byte[] b = IO.read( value.openInputStream());
		return Arrays.equals(a, b);
	}
}

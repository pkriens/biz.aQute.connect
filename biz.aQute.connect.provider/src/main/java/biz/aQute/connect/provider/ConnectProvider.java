package biz.aQute.connect.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;

public class ConnectProvider implements ModuleConnector {

	private List<String> bundles;

	@Override
	public void initialize(File storage, Map<String, String> configuration) {
		try {
			System.out.println("Storage " + storage);
			System.out.println("Config  " + configuration);
			URL bundlesURL = getClass().getClassLoader().getResource("BUNDLES/index");
			String bundleString = IO.collect(bundlesURL.openStream());
			bundles = Arrays.asList(bundleString.split("\\s*,\\s*"));

		} catch (IOException e) {
			e.printStackTrace();
			throw Exceptions.duck(e);
		}
	}

	@Override
	public Optional<ConnectModule> connect(String location) throws BundleException {
		try {
			System.out.println("Location " + location);

			int i = bundles.indexOf(location);
			if (i < 0)
				return Optional.empty();

			URL root = getClass().getClassLoader().getResource("BUNDLES/bundle-" + i + "/index");
			String bundleString = IO.collect(root.openStream());
			List<String> entries = 	Arrays.asList(bundleString.split("\\s*,\\s*"));

			return Optional.of(new ConnectModule() {

				@Override
				public ConnectContent getContent() throws IOException {
					System.out.println("getContent " + location);
					return new ConnectContent() {
						
						@Override
						public Optional<Map<String, String>> getHeaders() {
							return getEntry("META-INF/MANIFEST.MF").map( e -> {
								try {
									Manifest m = new Manifest(e.getInputStream());
									Map<String, String> map = m.getMainAttributes().entrySet().stream()
											.collect(Collectors.toMap( ee->ee.getKey().toString(), ee->ee.getValue().toString()));
									return map;
								} catch (IOException e1) {
									e1.printStackTrace();
									throw Exceptions.duck(e1);
								}
							});
						}

						@Override
						public Iterable<String> getEntries() throws IOException {
							return Collections.unmodifiableCollection(entries);
						}

						@Override
						public Optional<ConnectEntry> getEntry(String path) {
							if ( !entries.contains(path))
								return Optional.empty();
							
							String specialPath = "BUNDLES/bundle-"+i+"/" + path;
							URL tmproot = getClass().getClassLoader().getResource(specialPath);
							if (tmproot == null) {
								tmproot = getClass().getClassLoader().getResource(path);
								if ( tmproot == null) {
									System.err.println("Supposed to ahve this resource "+ path);
									return Optional.empty();
								}
							}
							URL root = tmproot;

							return Optional.of(new ConnectEntry() {

								@Override
								public String getName() {
									return path;
								}

								@Override
								public long getContentLength() {
									return -1L;
								}

								@Override
								public long getLastModified() {
									return 0L;
								}

								@Override
								public InputStream getInputStream() throws IOException {
									try {
										System.err.println("loading " + path);
										return root.openStream();
									} catch (Exception e) {
										e.printStackTrace();
										throw Exceptions.duck(e);
									}
								}
							});
						}

						@Override
						public Optional<ClassLoader> getClassLoader() {
							return Optional.of(getClass().getClassLoader());
						}

						@Override
						public void open() throws IOException {
							System.out.println("open " + location);
						}

						@Override
						public void close() throws IOException {
							System.out.println("close " + location);
						}
					};
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	@Override
	public Optional<BundleActivator> createBundleActivator() {
		System.out.println("BA");
		return Optional.empty();
	}

}

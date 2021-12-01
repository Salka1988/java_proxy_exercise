package rkn2019;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyBuilder {
	private String dumpPath = "";
	private Map<String, String> contentReplacements = new HashMap<>(),
			headerReplacements = new HashMap<>(),
			redirections = new HashMap<>();
	private String jsInjectPath = "";
	private List<String> stripDomains = new ArrayList<>();
	private String mitmCertificatePath = "";
	private boolean sopSwitch;

	public ProxyBuilder dumpTo(String dumpPath) {
		this.dumpPath = dumpPath;
		return this;
	}

	public ProxyBuilder injectJS(String jsPath) {
		jsInjectPath = jsPath;
		return this;
	}

	public ProxyBuilder setContentReplacements(String[] replacements) {
		putValuesToMap(replacements, contentReplacements, "^");
		return this;
	}

	public ProxyBuilder setHeaderReplacements(String[] replacements) {
		putValuesToMap(replacements, headerReplacements, ":");
		return this;
	}

	public ProxyBuilder setRedirections(String[] rdir) {
		putValuesToMap(rdir, redirections, "^");
		return this;
	}

	public ProxyBuilder setMitmCertificate(String certificatePath) {
		mitmCertificatePath = certificatePath;
		return this;
	}

	public ProxyBuilder setSOP(boolean sopSwitch) {
		this.sopSwitch = sopSwitch;
		return this;
	}

	public ProxyBuilder setStripDomains(String[] domains) {
		if (domains != null)
			stripDomains.addAll(Arrays.asList(domains));
		return this;
	}

	public Proxy build() {
		return new Proxy(dumpPath, jsInjectPath, headerReplacements, sopSwitch, contentReplacements, redirections, stripDomains, mitmCertificatePath);
	}

	private void putValuesToMap(String[] arr, Map<String, String> map, String separator) {
		if (arr != null) {
			for (int i = 0; i < arr.length; i ++) {
				String[] sp = arr[i].split(separator);
				if (sp.length == 2)
					map.put(sp[0], sp[1]);
			}
		}
	}
}

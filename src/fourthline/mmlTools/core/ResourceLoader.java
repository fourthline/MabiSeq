/*
 * Copyright (C) 2014 たんらる
 */

package fourthline.mmlTools.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import fourthline.mmlTools.core.ResourceLoader;


public class ResourceLoader extends Control {
	@Override
	public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) 
			throws IllegalAccessException, InstantiationException, IOException {
		String bundleName = toBundleName(baseName, locale);
		String resourceName = toResourceName(bundleName, "properties");
		InputStream stream = ResourceLoader.class.getResourceAsStream("/"+resourceName);
		if (stream != null) {
			return new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
		}

		return super.newBundle(baseName, locale, format, loader, reload);
	}
}
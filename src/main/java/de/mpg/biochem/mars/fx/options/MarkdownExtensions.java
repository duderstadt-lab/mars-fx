/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.mpg.biochem.mars.fx.options;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

import de.mpg.biochem.mars.fx.Messages;
import de.mpg.biochem.mars.fx.options.Options.RendererType;

/**
 * Markdown extensions
 *
 * @author Karl Tauber
 */
public class MarkdownExtensions {

	static final HashMap<String, String> displayNames = new HashMap<>();
	// static final HashMap<String, String> commonmarkExtClasses = new
	// HashMap<>();
	static final HashMap<String, String> flexmarkExtClasses = new HashMap<>();

	static {
		ResourceBundle bundle = ResourceBundle.getBundle(
			"de.mpg.biochem.mars.fx.MarkdownExtensions");
		for (String key : bundle.keySet()) {
			String value = bundle.getString(key);
			if (key.startsWith("flexmark.ext.")) flexmarkExtClasses.put(key.substring(
				"flexmark.ext.".length()), value);
		}

		HashSet<String> ids = new HashSet<>();
		ids.addAll(flexmarkExtClasses.keySet());
		for (String id : ids)
			displayNames.put(id, Messages.get("MarkdownExtensionsPane.ext." + id));
	}

	public static String[] ids() {
		return displayNames.keySet().toArray(new String[displayNames.size()]);
	}

	public static String displayName(String id) {
		return displayNames.get(id);
	}

	public static boolean isAvailable(RendererType rendererType, String id) {
		switch (rendererType) {
			case FlexMark:
				return flexmarkExtClasses.containsKey(id);
			default:
				return false;
		}
	}

	public static List<com.vladsch.flexmark.util.misc.Extension>
		getFlexmarkExtensions()
	{
		return createdExtensions(flexmarkExtClasses);
	}

	private static <E> ArrayList<E> createdExtensions(
		HashMap<String, String> extClasses)
	{
		ArrayList<E> extensions = new ArrayList<>();
		for (String markdownExtension : flexmarkExtClasses.keySet()) {
			try {
				Class<?> cls = Class.forName(flexmarkExtClasses.get(markdownExtension));
				Method createMethod = cls.getMethod("create");
				@SuppressWarnings("unchecked")
				E extension = (E) createMethod.invoke(null);
				extensions.add(extension);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return extensions;
	}
}

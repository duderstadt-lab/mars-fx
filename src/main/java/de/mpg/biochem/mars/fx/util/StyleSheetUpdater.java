/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
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
package de.mpg.biochem.mars.fx.util;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * http://stackoverflow.com/questions/24704515/in-javafx-8-can-i-provide-a-stylesheet-from-a-string
 */
public class StyleSheetUpdater {

	private String css;
	
	private static StringURLStreamHandlerFactory urlFactory;
	
    public StyleSheetUpdater() {
    	if (urlFactory == null) {
    		urlFactory = new StringURLStreamHandlerFactory();
        	URL.setURLStreamHandlerFactory(urlFactory);
    	}
    }
    /*
    public void addStyleSheet(Parent parent, String css) {
    	this.css = css;
    	
    	parent.getStylesheets().add("internal:"+System.nanoTime()+"stylesheet.css");
    }
    */
    public String getStyleSheetURL(String css) {
    	this.css = css;
    	
    	return "internal:"+System.nanoTime()+"stylesheet.css";
    }
    
	private class StringURLConnection extends URLConnection {
	    public StringURLConnection(URL url){
	        super(url);
	    }

	    @Override public void connect() throws IOException {}

	    @Override public InputStream getInputStream() throws IOException {
	        return new ByteArrayInputStream(css.getBytes("UTF-8"));
	    }
	}

	private class StringURLStreamHandlerFactory implements URLStreamHandlerFactory {
	    URLStreamHandler streamHandler = new URLStreamHandler(){
	        @Override protected URLConnection openConnection(URL url) throws IOException {
	            if (url.toString().toLowerCase().endsWith(".css")) {
	                return new StringURLConnection(url);
	            }
	            throw new FileNotFoundException();
	        }
	    };
	    @Override public URLStreamHandler createURLStreamHandler(String protocol) {
	        if ("internal".equals(protocol)) {
	            return streamHandler;
	        }
	        return null;
	    }
	}
}


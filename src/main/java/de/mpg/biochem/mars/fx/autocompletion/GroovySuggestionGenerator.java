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
package de.mpg.biochem.mars.fx.autocompletion;

import de.mpg.biochem.mars.molecule.AbstractMolecule;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.SingleMolecule;
import de.mpg.biochem.mars.molecule.SingleMoleculeArchive;
import de.mpg.biochem.mars.table.MarsTable;
import javafx.scene.control.IndexRange;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.ClassUtils;

public class GroovySuggestionGenerator {
	protected static HashMap<Class, List<CompletionItem>> autoCompletionClasses;
	protected static HashMap<String, Class> keywordToClassMap;
	
	protected static final Set<Character> LEGAL_CHARS;

	  static {
	    Set<Character> set = new HashSet<>();
	    for (char c = '0'; c <= '9'; c++) {
	      set.add(c);
	    }
	    for (char c = 'a'; c <= 'z'; c++) {
	      set.add(c);
	      set.add((char) (c - 'a' + 'A'));
	    }
	    set.add('-');
	    set.add('_');
	    LEGAL_CHARS = Collections.unmodifiableSet(set);
	  }
	
	private static GroovySuggestionGenerator instance = null;

    public static GroovySuggestionGenerator getInstance() {
        if (instance == null) {
            instance = new GroovySuggestionGenerator();
        }
        return instance;
    }

    private GroovySuggestionGenerator() {
    	autoCompletionClasses = new HashMap<Class, List<CompletionItem>>();
    	keywordToClassMap = new HashMap<String, Class>();
    	
    	addToAutoCompletionClasses(MarsTable.class, "Mars");
    	keywordToClassMap.put("table", MarsTable.class);
    	
    	addToAutoCompletionClasses(Stream.class, "JDK8");
    	keywordToClassMap.put("table.rows()", Stream.class);
    	
    	addToAutoCompletionClasses(SingleMolecule.class, "Mars");
    	keywordToClassMap.put("molecule", SingleMolecule.class);
    	
    	addToAutoCompletionClasses(SingleMoleculeArchive.class, "Mars");	
    	keywordToClassMap.put("archive", SingleMoleculeArchive.class);
    }

    private void addToAutoCompletionClasses(Class clazz, String library) {
    	List<CompletionItem> classItemList = new ArrayList<CompletionItem>();

        List<Class> allClasses = ClassUtils.getAllSuperclasses(clazz);
        allClasses.add(0, clazz);
        
        for (Class c : allClasses) {
        	Method[] methods = c.getDeclaredMethods();
	        for (Method method : methods) {
	        	
	        	if (!Modifier.isPublic(method.getModifiers()))
	        		continue;
	        	
	        	boolean overridden = false;
	        	for (CompletionItem item : classItemList) {
	        		if (item.getMethod().getName().equals(method.getName())) {
	        			if (method.getParameters().length == 0)
	        				overridden = true;
	        			else if (Arrays.equals(method.getParameterTypes(), item.getMethod().getParameterTypes())) {
	        				overridden = true;
	        			}
	        		}
	        	}
	        	
	        	if (overridden)
	        		continue;
	        	
	            String description = "<ul>";
	            
	            String shortDesc = method.getName() + "(";
	            String replacementText = method.getName() + "(";
	
	            String parameters = "-";
	            for (Parameter parameter : method.getParameters()) {
	                if (!shortDesc.endsWith("(")) {
	                    shortDesc = shortDesc + ", ";
	                }
	                shortDesc = shortDesc + parameter.getType().getSimpleName() + " " + parameter.getName();
	                
	                if (!replacementText.endsWith("(")) {
	                	replacementText = replacementText + ", ";
	                }
	                replacementText = replacementText + parameter.getName();
	                
	                description = description + "<li>" + parameter.getType().getSimpleName() + " " + parameter.getName() + "</li>";
	                parameters = parameters + parameter.getType().getCanonicalName() + "-";
	            }
	            shortDesc = shortDesc + ") : " + method.getReturnType().getSimpleName();
	            replacementText = replacementText + ")";
	            description = description + "</ul>";
	            
	            if (parameters.length() == 1) {
	                parameters = parameters + "-";
	            }
	
	            String classLink = getUrlPrefix(c.getCanonicalName(), library) + "/" + c.getCanonicalName().replace(".", "/") + ".html";
	            String methodLink = classLink + "#" + method.getName() + parameters;
	            ////System.out.println("Link: " + link);
	
	            description = "<a href=\"" + methodLink + "\">" + method.getName() + "</a><br>" + description;
	
	            description = description + "<br>" +
	                    "Defined in " +
	                    "<a href=\"" + methodLink + "\">" + c.getCanonicalName() + "</a><br>";
	
	            description = description + "<br>" +
	                    "returns " + method.getReturnType().getSimpleName();
	
	            classItemList.add(new CompletionItem(method, replacementText, shortDesc, description));
	        }
        }
        autoCompletionClasses.put(clazz, classItemList);
    }
    
    public Collection<CompletionItem> getSuggestions(String text, int caretPos, String keyword) {
    	if (!keywordToClassMap.containsKey(keyword))
    		return Collections.emptyList();
    	
	    IndexRange replaceRange = getReplaceRange(text, caretPos);
	    char c = ' ';
	    int index = caretPos;
	    while (index > 0) {
	     try {
	    	  c = text.charAt(index);
		      if (c == ' ' || c == '\t' || c == '\n') {
		        break;
		      }
	     } catch(StringIndexOutOfBoundsException e) {
	    	 //Do nothing... Just continue.
	    	 //Sometimes this happens at the very end of the 
	    	 //codeArea I am not sure why...
	     }
	      index--;
	    }
	    
	    String fragment = text.substring(replaceRange.getStart(), replaceRange.getEnd());

	    if (fragment.equals(keyword))
	    	return getListForKeyword(keyword).stream().collect(Collectors.toList());
	    else 
	    	return getListForKeyword(keyword).stream().filter(item -> item.getCompletionText().contains(fragment)).collect(Collectors.toList());
	}
    
    private List<CompletionItem> getListForKeyword(String keyword) {
    	return autoCompletionClasses.get(keywordToClassMap.get(keyword));
    }
    
    public IndexRange getReplaceRange(String text, int caretPos) {
        int start = caretPos - 1;
        int end = caretPos;
        while (start > -1 && LEGAL_CHARS.contains(text.charAt(start))) {
          start--;
        }

        while (end < text.length() && LEGAL_CHARS.contains(text.charAt(end))) {
          end++;
        }

        return new IndexRange(start + 1, end);
    }

    protected boolean isValidChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '.' || ch == '"' || ch == '(' ||  ch == ')';
    }

    String getUrlPrefix(String canonicalClassName, String defaultLibrary) {
        if (canonicalClassName.startsWith("org.scijava.")) {
            return "https://javadoc.scijava.org/SciJava";
        } else if (canonicalClassName.startsWith("net.imagej.")) {
            return "https://javadoc.scijava.org/ImageJ";
        } else if (canonicalClassName.startsWith("ij.")) {
            return "https://javadoc.scijava.org/ImageJ1";
        } else if (canonicalClassName.startsWith("net.imglib2.")) {
            return "https://javadoc.scijava.org/ImgLib2";
        } else if (canonicalClassName.startsWith("java.awt.")) {
            return "https://docs.oracle.com/javase/8/docs/api";
        } else {
            return "https://javadoc.scijava.org/" + defaultLibrary;
        }
    }
}

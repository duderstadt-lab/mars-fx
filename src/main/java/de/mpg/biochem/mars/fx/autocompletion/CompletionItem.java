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

import java.lang.reflect.Method;

/**
 * @author Karl Duderstadt - small edits for javafx framework autocompletion.,,
 */
public class CompletionItem {

	private String completionText;
	private String shortDesc;
	private String summary;
	private Method method;

	/**
	 * Constructor.
	 *
	 * @param method The method.
	 * @param completionText The text to replace.
	 */
	public CompletionItem(Method method, String completionText){
		this(method, completionText, null);
	}

	/**
	 * Constructor.
	 *
	 * @param method The method.
	 * @param completionText The text to replace.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 */
	public CompletionItem(Method method, String completionText,
							String shortDesc) {
		this(method, completionText, shortDesc, null);
	}


	/**
	 * Constructor.
	 *
	 * @param method The method.
	 * @param completionText The text to replace.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 * @param summary The summary of this completion.  This should be HTML.
	 *        This may be <code>null</code>.
	 */
	public CompletionItem(Method method, String completionText,
							String shortDesc, String summary) {
		this.method = method;
		this.completionText = completionText;
		this.shortDesc = shortDesc;
		this.summary = summary;
	}


	public String getCompletionText() {
		return completionText;
	}

	/**
	 * Returns the short description of this completion, usually used in
	 * the completion choices list.
	 *
	 * @return The short description, or <code>null</code> if there is none.
	 * @see #setShortDescription(String)
	 */
	public String getShortDescription() {
		return shortDesc;
	}


	public String getSummary() {
		return summary;
	}
	
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Sets the short description of this completion.
	 *
	 * @param shortDesc The short description of this completion.
	 * @see #getShortDescription()
	 */
	public void setShortDescription(String shortDesc) {
		this.shortDesc = shortDesc;
	}


	/**
	 * Sets the summary for this completion.
	 *
	 * @param summary The summary for this completion.
	 * @see #getSummary()
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

}

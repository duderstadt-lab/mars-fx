package de.mpg.biochem.mars.fx.autocompletion;

import java.lang.reflect.Method;

/**
 * 
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
	 * @param completionText The text to replace.
	 */
	public CompletionItem(Method method, String completionText){
		this(method, completionText, null);
	}

	/**
	 * Constructor.
	 *
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
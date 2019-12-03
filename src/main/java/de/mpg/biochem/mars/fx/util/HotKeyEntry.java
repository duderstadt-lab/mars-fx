package de.mpg.biochem.mars.fx.util;

import javafx.scene.input.KeyCombination;

public class HotKeyEntry {
	String shortcut;
	String tag;
	
	public HotKeyEntry(String shortcut) {
		this.shortcut = shortcut;
		this.tag = "tag";
	}
	
	public HotKeyEntry(String shortcut, String tag) {
		this.shortcut = shortcut;
		this.tag = tag;
	}
	
	//Getters and Setters
	public String getShortcut() {
		return shortcut;
	}
	
	public void setShortcut(String shortcut) {
		this.shortcut = shortcut;
	}
	
	public String getTag() {
		return tag;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public KeyCombination getKeyCombination() {
		return KeyCombination.valueOf(shortcut);
	}
	
	public boolean validate() {
		
		//Check is this is valid
		KeyCombination kc = KeyCombination.valueOf(shortcut);
		
		if(kc == null)
			System.out.println("Failed to create KeyCombination");
		
		if(kc != null)
			return true;
		
		return false;
	}
}
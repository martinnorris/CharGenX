package generator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.nio.file.Path;

import javax.swing.JComponent;

public interface Options
{
	public Dimension getInitialSize();
	
	public enum OptionsColourEnum {_SELECTED, _SUGGESTED, _EXCLUDED, _WARNING, _ERROR, _TEXT, _BACK}
	public Color getColour(OptionsColourEnum enumColour);
	public Path getResources();
	
	public enum OptionsFontEnum {_CHARACTER, _TAB, _CATEGORY, _SELECTION, _TEXT}
	public Font getFont(Font fontSource, OptionsFontEnum enumPurpose);
	public Font getFont(JComponent componentSource, OptionsFontEnum enumPurpose);
}


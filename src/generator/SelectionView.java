package generator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import generator.Options.OptionsColourEnum;
import generator.Options.OptionsFontEnum;

public interface SelectionView
{
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints);
	public String extractMembers(JComponent componentView, ParseArgument argument);
	public String applyHints(SelectionData dataSelection, String scHints);
	
	public SelectionData dataSelection();
	public JComponent updatePanel(SelectionData dataSelection, Options optionsView, String scHints);
}

class SelectionViewBase extends JLabel implements SelectionView
{
	private static final long serialVersionUID = -4902802792544873962L;
	
	public SelectionViewBase() 
	{
	}
	
	protected SelectionData m_dataSelection = null;
	
	protected String getValue()
	{
		return m_dataSelection.getValue().toString();
	}
	
	@Override
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		m_dataSelection = dataSelection;
		
		setText(getValue());
		setFont(optionsView.getFont(this, OptionsFontEnum._SELECTION));
		setBackground(chooseColour(optionsView, dataSelection, Color.WHITE));
		// Overwrite parameters of the component created
		setArguments(dataSelection, scHints);
		
		return this;
	}

	protected String setArguments(SelectionData dataSelection, String scHints)
	{
		return SelectionViewBase.extractMembers(this, this, "base", scHints);		
	}
	
	@Override
	public String applyHints(SelectionData dataSelection, String scHints)
	{
		return scHints;
	}

	@Override
	public SelectionData dataSelection()
	{
		return m_dataSelection;
	}
	
	@Override
	public JComponent updatePanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		setText(getValue());
		setBackground(chooseColour(optionsView, dataSelection, Color.WHITE));
		return this;
	}
	
	public static final String extractMembers(SelectionView viewSelection, JComponent componentView, String scStart, String scHint)
	{
		// Cut off subsequent hints
		int iIndexNext = scHint.indexOf('_');
		if (0<=iIndexNext) scHint = scHint.substring(0, iIndexNext);

		// Check for key expected start
		ParseArgument argument = new ParseArgument(scHint);
		if (!argument.hasSymbol()) return scHint;
		String scSymbol = argument.getStringRegex();
		if (!(scSymbol.startsWith(scStart))) return scHint;
		if (!argument.hasSymbol()) return scHint;
		
		return viewSelection.extractMembers(componentView, argument);
	}
	
	enum EnumBaseParse {_KEY, _ALIGNMENT, _FONT};
	
	public static final String extractCommon(SelectionView viewSelection, JComponent componentView, ParseArgument argument)
	{
		EnumBaseParse baseParse = EnumBaseParse._KEY;
		String scKey = "";
		
		while (argument.hasSymbol())
		{
			switch (baseParse)
			{
			case _KEY:
				// After _value or _label or _name look for key word
				scKey = argument.getStringRegex();
				if (scKey.startsWith("OPAQUE")) { componentView.setOpaque(true); continue; } // No need to change state
				if (scKey.startsWith("ALIGN")) { baseParse = EnumBaseParse._ALIGNMENT; continue; }
				if (scKey.startsWith("FONT")) { baseParse = EnumBaseParse._FONT; continue; }
				// Next argument not recognised
				break;
				
			case _ALIGNMENT:
				baseParse = EnumBaseParse._KEY;
				
				if (argument.isNumberNext())
				{
					String scNumber = argument.getNumber();
					Float fAlignmentX = 0.5f;
					
					if (argument.isDecimal()) 
						fAlignmentX = Float.parseFloat(scNumber);
					else 
						fAlignmentX = (float)Integer.parseInt(scNumber);
					
					componentView.setAlignmentX(fAlignmentX);
					continue;				
				}
				
				String scAlignment = argument.getStringRegex();
				if (scAlignment.startsWith("L")) componentView.setAlignmentX(LEFT_ALIGNMENT);
				if (scAlignment.startsWith("C")) componentView.setAlignmentX(CENTER_ALIGNMENT);
				if (scAlignment.startsWith("R")) componentView.setAlignmentX(RIGHT_ALIGNMENT);
				continue;
				
			case _FONT:
				baseParse = EnumBaseParse._KEY;
				
				if (argument.isNumberNext())
				{
					String scNumber = argument.getNumber();
					Font fontBase = componentView.getFont();
					int iFontSize = fontBase.getSize();
					int iCreateSize = Integer.parseInt(scNumber);
					if (iCreateSize==iFontSize) continue;
					float fFontSize = iCreateSize;
					Font fontCreated = fontBase.deriveFont(fFontSize);
					componentView.setFont(fontCreated);
				}
				continue;
				
			default:
				break;	
			}
			
			// Nothing else to do
			break;
		}
		
		return scKey;
	}
	
	@Override
	public String extractMembers(JComponent componentView, ParseArgument argument)
	{
		// Try and extract anything special for the component then get the common component configurations
		return extractCommon(this, componentView, argument);
	}
	
	public static final Color chooseColour(Options optionsView, SelectionData dataSelection, Color colourDefault)
	{
		boolean zSelected = dataSelection.isSelected();
		boolean zExcluded = dataSelection.isExcluded();
		boolean zSuggested = dataSelection.isSuggested();
		
		if (zSelected && zExcluded)
		{
			return optionsView.getColour(OptionsColourEnum._ERROR);
		}
		
		if (zSelected && !zSuggested)
		{
			return optionsView.getColour(OptionsColourEnum._WARNING);
		}
		
		if (zSelected)
		{
			return optionsView.getColour(OptionsColourEnum._SELECTED);
		}
		
		if (zExcluded)
		{
			return optionsView.getColour(OptionsColourEnum._EXCLUDED);
		}
		
		if (zSuggested)
		{
			return optionsView.getColour(OptionsColourEnum._SUGGESTED);
		}
		
		return colourDefault;
	}	

	@Override
	public String toString()
	{
		return "View of " + m_dataSelection.toString();
	}
}

class SelectionViewName extends SelectionViewBase
{
	private static final long serialVersionUID = -6443193214008971955L;
	
	@Override
	protected String getValue()
	{
		return m_dataSelection.getName();
	}
	
	@Override
	protected String setArguments(SelectionData dataSelection, String scHints)
	{
		return SelectionViewBase.extractMembers(this, this, "name", scHints);		
	}	
}

class SelectionViewValue extends SelectionViewBase
{
	private static final long serialVersionUID = -5688432534250782202L;

	protected String TYPE = "value";

	@Override
	protected String getValue()
	{
		try
		{
			return m_dataSelection.getValue().toString();
		}
		catch (NumberFormatException x)
		{
			setBorder(BorderFactory.createLineBorder(Color.magenta.darker(), 3));
			return x.toString();
		}
	}
	
	@Override
	protected String setArguments(SelectionData dataSelection, String scHints)
	{
		return SelectionViewBase.extractMembers(this, this, "value", scHints);		
	}	
}

class SelectionViewLabel extends SelectionViewBase
{
	private static final long serialVersionUID = -2952701440424988791L;

	@Override
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		m_dataSelection = dataSelection;
		
		setText(getValue());
		setFont(optionsView.getFont(this, OptionsFontEnum._TEXT));
		setBackground(chooseColour(optionsView, dataSelection, Color.WHITE));
		// Overwrite parameters of the label created
		SelectionViewBase.extractMembers(this, this, "label", scHints);
		
		return this;
	}

	@Override
	public String extractMembers(JComponent componentView, ParseArgument argument)
	{
		// After _button next symbol is the label
		String scLabel = argument.getStringRegex();
		setText(scLabel);
		
		// Check for all the common things that can be set on a component
		return SelectionViewBase.extractCommon(this, componentView, argument);		
	}
	
	@Override
	public String applyHints(SelectionData dataSelection, String scHints)
	{
		return "";
	}	
}

class SelectionViewMultipleItem
{
	SelectionViewMultipleItem(SelectionView viewSelection, String scHint)
	{
		m_viewSelection = viewSelection;
		m_scHint = scHint;			
	}
	public SelectionView m_viewSelection;
	public String m_scHint;
}

class SelectionViewMultiple extends JPanel implements SelectionView
{
	private static final long serialVersionUID = -3962267025273779966L;
	
	public SelectionViewMultiple(ViewHelper helperView, ActionListener listenerAction)
	{
		m_helperView = helperView;
		m_listenerAction = listenerAction;
	}
	
	@Override
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		ArrayList<SelectionViewMultipleItem> listSelection = breakdownViews(m_listenerAction, scHints);
		if (0==listSelection.size()) return buildPanelName(dataSelection, optionsView, scHints);
		if (1==listSelection.size()) return buildPanelSingle(listSelection.get(0), dataSelection, optionsView, scHints);
		return buildPanelMultiple(listSelection, dataSelection, optionsView, scHints);
	}

	@Override
	public String extractMembers(JComponent componentView, ParseArgument argument)
	{
		return null;
	}
	
	@Override
	public String applyHints(SelectionData dataSelection, String scHints) 
	{
		return m_scHints;
	}	
	
	@Override
	public SelectionData dataSelection()
	{
		Component[] aContent = getComponents();
		
		for (Component content : aContent)
		{
			if (!(content instanceof SelectionView)) continue;
			SelectionView view = (SelectionView) content;
			return view.dataSelection();
		}
		
		return null;
	}
	
	@Override
	public JComponent updatePanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		Component[] aContent = getComponents();
		
		for (Component content : aContent)
		{
			if (!(content instanceof SelectionView)) continue;
			SelectionView view = (SelectionView) content;
			SelectionData dataView = view.dataSelection();
			if (!dataSelection.sameSelection(dataView)) continue;
			view.updatePanel(dataSelection, optionsView, scHints);			
		}
		return this;
	}
	
	private ViewHelper m_helperView = null;
	private ActionListener m_listenerAction = null;
	private String m_scHints = null; 

	private ArrayList<SelectionViewMultipleItem> breakdownViews(ActionListener listenerAction, String scHint)
	{
		ArrayList<SelectionViewMultipleItem> listSelection = new ArrayList<SelectionViewMultipleItem>();
		
		SelectionViewMultipleItem item = null;
		
		while (true)
		{
			int iIndexView = scHint.indexOf('_');
			if (0<=iIndexView) scHint = scHint.substring(iIndexView+1);
			
			SelectionView viewSelectionNext = getSelectionView(scHint, listenerAction);
			if (null==viewSelectionNext) break;
			
			item = new SelectionViewMultipleItem(viewSelectionNext, scHint);
			listSelection.add(item);
			
			int iIndexNext = scHint.indexOf('_');
			if (0>iIndexNext) scHint = "";
		}
		
		// Any hints left over
		m_scHints = scHint;
		
		return listSelection;
	}
	
	private SelectionView getSelectionView(String scView, ActionListener listenerAction)
	{
		if (scView.startsWith("name")) return new SelectionViewName();
		if (scView.startsWith("value")) return new SelectionViewValue();
		if (scView.startsWith("label")) return new SelectionViewLabel(); // displays value by default
		if (scView.startsWith("button")) return new SelectionViewButton(m_helperView, listenerAction);
		if (scView.startsWith("graphic")) return new SelectionViewGraphic();
		if (scView.startsWith("field")) return new SelectionViewField(listenerAction);
		if (scView.startsWith("area")) return new SelectionViewArea(listenerAction);
		return null;
	}
	
	private JComponent buildPanelName(SelectionData dataSelection, Options optionsView, String scHints)
	{
		SelectionViewName viewReturn = new SelectionViewName();
		return viewReturn.buildPanel(dataSelection, optionsView, scHints);
	}
	
	private JComponent buildPanelSingle(SelectionViewMultipleItem item, SelectionData dataSelection, Options optionsView, String scHints)
	{
		SelectionView viewReturn = item.m_viewSelection;
		return viewReturn.buildPanel(dataSelection, optionsView, item.m_scHint);
	}
	
	protected JComponent buildPanelMultiple(ArrayList<SelectionViewMultipleItem> listSelection, SelectionData dataSelection, Options optionsView, String scHints) 
	{
		//setBorder(BorderFactory.createLineBorder(Color.MAGENTA.brighter(), 3));
		
		Iterator<SelectionViewMultipleItem> iterateList = listSelection.iterator();
		
		while (iterateList.hasNext())
		{
			SelectionViewMultipleItem item = iterateList.next();
			if (!m_helperView.buildContent(dataSelection, item.m_viewSelection, this, item.m_scHint)) throw new RuntimeException(String.format("Could not build %d content for %s in %s", listSelection.size(), dataSelection, item.m_scHint));
		}
		
		return this;
	}
	
	@Override
	public String toString()
	{
		return "Multiple views";
	}
}

class SelectionViewMultipleLabel extends SelectionViewMultiple
{
	private static final long serialVersionUID = -812019704098920695L;

	public SelectionViewMultipleLabel(ViewHelper helperView, ActionListener listenerAction)
	{
		super(helperView, listenerAction);
	}
	
	@Override
	protected JComponent buildPanelMultiple(ArrayList<SelectionViewMultipleItem> listSelection, SelectionData dataSelection, Options optionsView, String scHints)
	{
		throw new RuntimeException(String.format("Cannot add %s to container with multiple views", dataSelection.getName(), scHints));
	}
}

class SelectionViewField extends JPanel implements SelectionView
{
	private static final long serialVersionUID = 2610908827657997166L;

	public SelectionViewField(ActionListener listenerAction)
	{
		m_listenerAction = listenerAction;
	}
	
	private SelectionData m_dataSelection = null;
	private ActionListener m_listenerAction = null;
	
	private JTextField m_fieldContent = null;
	
	@Override
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		setLayout(new FlowLayout(FlowLayout.LEFT));
		
		// Set the members
		m_dataSelection = dataSelection;
		Color colourBack = optionsView.getColour(OptionsColourEnum._BACK);
		
		SelectionViewName labelName = new SelectionViewName();
		labelName = (SelectionViewName)labelName.buildPanel(dataSelection, optionsView, scHints);
		labelName.setOpaque(true);
		labelName.setBackground(colourBack);
		
		m_fieldContent = new JTextField();
		m_fieldContent.setText(m_dataSelection.getValue().toString());
		m_fieldContent.setOpaque(true);
		m_fieldContent.setBackground(colourBack);
		m_fieldContent.setFont(optionsView.getFont(m_fieldContent, OptionsFontEnum._TEXT));
		m_fieldContent.setColumns(32);
		m_fieldContent.addActionListener(adaptFieldListener(dataSelection, m_listenerAction));
		
		add(labelName);
		add(m_fieldContent, BorderLayout.CENTER);
		
		return this;		
	}
	
	@Override
	public String extractMembers(JComponent componentView, ParseArgument argument)
	{
		return null;
	}
	
	@Override
	public String applyHints(SelectionData dataSelection, String scHints) 
	{
		return scHints;
	}
	
	@Override
	public SelectionData dataSelection()
	{
		return m_dataSelection;
	}
	
	@Override
	public JComponent updatePanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		m_fieldContent.setText(m_dataSelection.getValue().toString());
		m_fieldContent.setBackground(SelectionViewBase.chooseColour(optionsView, dataSelection, Color.WHITE));
		return this;
	}
	
	private ActionListener adaptFieldListener(final SelectionData dataSelection, final ActionListener listenerAction)
	{
		ActionListener listenerField = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent eventField) 
			{
				JTextField componentSource = (JTextField)eventField.getSource();
				
				CategoryData dataCategory = dataSelection.getParent();
				String scAction = String.format("Value '%s' '%s' '%s' ", dataCategory.getName(), dataSelection.getName(), componentSource.getText());				
				ActionEvent eventAction = new ActionEvent(dataSelection, ActionEvent.ACTION_PERFORMED, scAction);
				listenerAction.actionPerformed(eventAction);
				
				return;
			}

		};
		
		return listenerField;
	}
}

class SelectionViewArea extends JPanel implements SelectionView
{
	private static final long serialVersionUID = -8063080322420881486L;

	public SelectionViewArea(ActionListener listenerAction)
	{
		m_listenerAction = listenerAction;
	}
	
	private SelectionData m_dataSelection = null;
	private ActionListener m_listenerAction = null;
	
	private JTextArea m_areaContent = null;
	
	@Override
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		// Set the members
		m_dataSelection = dataSelection;
		Color colourBack = optionsView.getColour(OptionsColourEnum._BACK);
		
		m_areaContent = new JTextArea();
		m_areaContent.setOpaque(true);
		m_areaContent.setBackground(colourBack);
		m_areaContent.setFont(optionsView.getFont(m_areaContent, OptionsFontEnum._TEXT));
		m_areaContent.setColumns(32);
		m_areaContent.setRows(6);
		m_areaContent.setLineWrap(true);
		m_areaContent.setWrapStyleWord(true);
		// Set the members - the remainder of the hints could be returned for hints when adding to the container
		SelectionViewBase.extractMembers(this, m_areaContent, "area", scHints);
		// Set initial content
		m_areaContent.setText(m_dataSelection.getValue().toString());
		
		Document documentContent = m_areaContent.getDocument();
		documentContent.addDocumentListener(adaptDocumentListener(dataSelection, m_listenerAction));
		
		JScrollPane paneArea = new JScrollPane(m_areaContent);
		add(paneArea);
		
		return this;		
	}
	
	@Override
	public JComponent updatePanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		// Does not update document
		//m_areaContent.setText(m_dataSelection.getValue().toString());
		m_areaContent.setBackground(SelectionViewBase.chooseColour(optionsView, dataSelection, Color.WHITE));
		return this;
	}
	
	private DocumentListener adaptDocumentListener(final SelectionData dataSelection, final ActionListener listenerAction)
	{
		DocumentListener listenerArea = new DocumentListener()
		{
			@Override
			public void changedUpdate(DocumentEvent eventDocument) 
			{
				fireActionEvent(eventDocument.getDocument());
			}

			@Override
			public void insertUpdate(DocumentEvent eventDocument) 
			{
				fireActionEvent(eventDocument.getDocument());
			}

			@Override
			public void removeUpdate(DocumentEvent eventDocument) 
			{
				fireActionEvent(eventDocument.getDocument());
			}
			
			private void fireActionEvent(Document document)
			{
				try
				{
					int iLength = document.getLength();
					String scText = document.getText(0, iLength);
					CategoryData dataCategory = dataSelection.getParent();
					String scAction = String.format("Value '%s' '%s' '%s' ", dataCategory.getName(), dataSelection.getName(), scText);				
					ActionEvent eventAction = new ActionEvent(dataSelection, ActionEvent.ACTION_PERFORMED, scAction);
					listenerAction.actionPerformed(eventAction);
				}
				catch (BadLocationException x)
				{
					
				}
				return;
			}
		};
		
		return listenerArea;
	}
	
	@Override
	public String applyHints(SelectionData dataSelection, String scHints) 
	{
		return scHints;
	}

	@Override
	public String extractMembers(JComponent componentView, ParseArgument argument) 
	{
		// Check for all the common things that can be set on a component
		String scKey = SelectionViewBase.extractCommon(this, componentView, argument);
		
		if (scKey.equals("SIZE"))
		{
			String scSize = argument.getStringRegex();
			int iSeparator = scSize.indexOf(',');
			if (0<iSeparator)
			{
				int iRows = Integer.parseInt(scSize.substring(iSeparator+1));
				m_areaContent.setRows(iRows);
				scSize = scSize.substring(0, iSeparator);
			}
			int iColumns = Integer.parseInt(scSize);
			m_areaContent.setColumns(iColumns);
		}
		
		return argument.remainder();		
	}

	@Override
	public SelectionData dataSelection()
	{
		return m_dataSelection;
	}
}

class SelectionViewButton extends JButton implements SelectionView
{
	private static final long serialVersionUID = 7125294764249783769L;

	public SelectionViewButton(ViewHelper helperView, ActionListener listenerAction)
	{
		m_helperView = helperView;
		m_listenerAction = listenerAction;
	}
	
	private SelectionData m_dataSelection = null;
	
	private ViewHelper m_helperView = null;
	private ActionListener m_listenerAction = null;
	
	private SelectionData m_dataSelectionTarget = null;
	private String m_scExpression = null;
	private String m_scHints = null;
	
	@Override
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		m_dataSelection = dataSelection;
		
		// Create default button
		setText(dataSelection.getName());
		// Set the members - the remainder of the hints could be returned for hints when adding to the container
		m_scHints = SelectionViewBase.extractMembers(this, this, "button", scHints);
		
		// Create the button
		addActionListener(adaptFieldListener(m_dataSelectionTarget, m_listenerAction, m_scExpression));
		
		return this;		
	}
	
	enum EnumButtonParse {_TEXT, _EXPRESSION, _BUILD};
	
	@Override
	public String extractMembers(JComponent componentView, ParseArgument argument)
	{
		// After _button next symbol is the label
		String scLabel = argument.getStringRegex();
		setText(scLabel);
		
		// Check for all the common things that can be set on a component
		String scKey = SelectionViewBase.extractCommon(this, componentView, argument);
		
		String scFirst = null;
		String scSecond = null;
		
		// Check for setting the expression
		while (scKey.startsWith("="))
		{
			String scPart = argument.partEnding('=');
			if (0==scPart.length()) break;
			
			// If only 1 expression then first is null and should target the same selection
			scFirst = scSecond;
			// If have 2 expressions the first is the selection to set 
			scSecond = scPart;
			
			// Malformed if not next '='
			if ('='!=argument.nextChar()) break;
			scKey = argument.getStringRegex();			
		}
		
		m_scExpression = scSecond;
		
		if (null==scFirst) 
			m_dataSelectionTarget = m_dataSelection;
		else
			m_dataSelectionTarget = m_helperView.findSelectionData(scFirst);
		
		return argument.remainder();		
	}
	
	@Override
	public String applyHints(SelectionData dataSelection, String scHints) 
	{
		return m_scHints;
	}
	
	@Override
	public SelectionData dataSelection()
	{
		return m_dataSelection;
	}
	
	@Override
	public JComponent updatePanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		return this;
	}
	
	private ActionListener adaptFieldListener(final SelectionData dataSelection, final ActionListener listenerAction, final String scExpression)
	{
		ActionListener listenerField = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent eventField) 
			{
				CategoryData dataCategory = dataSelection.getParent();
				String scAction = String.format("Expression '%s' '%s' '%s' ", dataCategory.getName(), dataSelection.getName(), scExpression);				
				ActionEvent eventAction = new ActionEvent(dataSelection, ActionEvent.ACTION_PERFORMED, scAction);
				listenerAction.actionPerformed(eventAction);
				
				return;
			}
		};
		
		return listenerField;
	}
}

class SelectionViewGraphic extends SelectionViewBase
{
	private static final long serialVersionUID = 7320940005026722276L;
	
	protected Options m_optionsView = null;
	
	// Used by derived classes when they are instantiated
	protected void setMembers(SelectionData dataSelection, Options optionsView)
	{
		m_dataSelection = dataSelection;
		m_optionsView = optionsView;		
	}
	
	@Override
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		m_dataSelection = dataSelection;
		String scName = dataSelection.getName();
		int iSuffix = scName.indexOf("_graphic");
		if (0>iSuffix) iSuffix = scName.length();
		String scGraphic = scName.substring(0, iSuffix);
		
		SelectionViewGraphic viewSelectionGraphic = getView(optionsView, scGraphic);
		viewSelectionGraphic.setMembers(dataSelection, optionsView);
		
		return viewSelectionGraphic.buildPanel(dataSelection, optionsView, scHints);
	}

	@Override
	public JComponent updatePanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		// Only set the colour
		setBackground(chooseColour(optionsView, dataSelection, Color.WHITE));
		return this;
	}
	
	private SelectionViewGraphic getView(Options optionsView, String scGraphic)
	{
		SelectionViewGraphic viewSelectionGraphic;
		
		viewSelectionGraphic = getGraphicClass(scGraphic);
		if (null!=viewSelectionGraphic) return viewSelectionGraphic;
		
		viewSelectionGraphic = getLoadedClass(optionsView, scGraphic);
		if (null!=viewSelectionGraphic) return viewSelectionGraphic;
		
		return new SelectionViewGraphicBase();		
	}
	
	private SelectionViewGraphic getGraphicClass(String scName)
	{
		String scClass = "generator.SelectionView_graphic_" + scName;
		
		try 
		{
			@SuppressWarnings("rawtypes")
			Class classGraphic = Class.forName(scClass);
			Object objectSelection = classGraphic.newInstance();
			SelectionViewGraphic viewSelection = (SelectionViewGraphic)objectSelection;
			return viewSelection;
		} 
		catch (ClassNotFoundException e) 
		{
		} 
		catch (InstantiationException e) 
		{
		} 
		catch (IllegalAccessException e) 
		{
		}
		catch (ClassCastException e)
		{
		}
		
		if ("down"==scName) return new SelectionView_graphic_down();
		if ("reset"==scName) return new SelectionView_graphic_reset();
		if ("up"==scName) return new SelectionView_graphic_up();
		
		return null;
	}
	
	private SelectionViewGraphic getLoadedClass(Options optionsView, String scName)
	{
		Path pathLoaded = optionsView.getResources();
		pathLoaded = pathLoaded.resolve(scName + ".png");
		String scGraphic = pathLoaded.toString();
		
		ImageIcon imageLoad = new ImageIcon(scGraphic);
		
		SelectionViewGraphicIcon viewSelection = new SelectionViewGraphicIcon(imageLoad);
		return viewSelection;
	}
	
	@Override
	public String toString()
	{
		return "Graphic " + m_dataSelection.toString();
	}
}

class SelectionViewGraphicBase extends SelectionViewGraphic implements Icon
{
	private static final long serialVersionUID = 7748539100517155736L;

	// Must have an argument free constructor so easy to create class by name
	public SelectionViewGraphicBase()
	{
	}	
	
	@Override
	public JComponent buildPanel(SelectionData dataSelection, Options optionsView, String scHints)
	{
		setIcon(this);
		return this;
	}
	
	public int getIconHeight() 
	{
		return 50;
	}

	public int getIconWidth() 
	{
		return 50;
	}

	@Override
	public void paintIcon(Component componentContainer, Graphics g, int iX, int iY) 
	{
		Color colourSelection = SelectionViewBase.chooseColour(m_optionsView, m_dataSelection, Color.BLACK);
		g.setColor(colourSelection);		
		Dimension dimensionPanel = componentContainer.getSize();
		g.fillRect(0, 0,  dimensionPanel.width,  dimensionPanel.height);
	}
}

class SelectionViewGraphicIcon extends SelectionViewGraphicBase
{
	private static final long serialVersionUID = -5299014565749766363L;

	public SelectionViewGraphicIcon(Icon iconLoaded)
	{
		m_icon = iconLoaded;
	}
	
	private Icon m_icon = null;
	
	@Override
	public int getIconHeight() 
	{
		return m_icon.getIconHeight();
	}

	@Override
	public int getIconWidth() 
	{
		return m_icon.getIconWidth();
	}

	@Override
	public void paintIcon(Component component, Graphics graphics, int iX, int iY) 
	{
		m_icon.paintIcon(component, graphics, iX, iY);
		return;
	}	
}
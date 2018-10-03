package generator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.nio.file.Path;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;

import generator.CharacterEvent.EventEditEnum;
import generator.CharacterEvent.EventViewEnum;

class CharacterView extends CharacterListenerAdapter implements Options
{
	CharacterView(CharacterData dataCharacter, Controls controlChar, File fileLoaded)
	{
		m_controlChar = controlChar;
		m_dataCharacter = dataCharacter;
		
		m_helperView = new ViewHelperList(dataCharacter, this, controlChar);
		
		setResources(fileLoaded);
	}
	
	private Controls m_controlChar = null;
	private CharacterData m_dataCharacter = null;
	
	private ViewHelper m_helperView = null;
	
	private JComponent m_containerChar = null;	
	private Path m_pathResources = null;
	
	private DefaultMutableTreeNode m_nodeBorder = null;
	
	public void createPanel()
	{
		m_containerChar = new JPanel();
		m_containerChar.setLayout(new BorderLayout());
		
		JComponent componentPanel = buildPanel(m_controlChar, m_containerChar, m_dataCharacter);
		JMenuBar barMenu = buildMenu();
		
		ViewHelper.frameView(componentPanel, barMenu, "Character View", getInitialSize());
		return;
	}
	
	private JComponent buildPanel(Controls controls, JComponent container, CharacterData dataCharacter)
	{
		ViewHelper helperView = new ViewHelperList(dataCharacter, this, controls);
		return helperView.createPanel(container);
	}
	
	private JMenuBar buildMenu()
	{
		JMenu menuFile = new JMenu("File");	
		JMenuItem menuitemFileExit = new JMenuItem("Exit");
		menuitemFileExit.addActionListener(m_controlChar);
		menuFile.add(menuitemFileExit);
		menuFile.addSeparator();
		JMenuItem menuitemFileNew = new JMenuItem("New");
		menuitemFileNew.addActionListener(m_controlChar);
		menuFile.add(menuitemFileNew);
		JMenuItem menuitemFileBlank = new JMenuItem("Blank");
		menuitemFileBlank.addActionListener(m_controlChar);
		menuFile.add(menuitemFileBlank);
		menuFile.addSeparator();
		
		JMenu menuLoadAll = new JMenu("Load");
		JMenuItem menuitemFileLoadData = new JMenuItem("LoadData");
		menuitemFileLoadData.addActionListener(m_controlChar);
		menuLoadAll.add(menuitemFileLoadData);
		JMenuItem menuitemFileLoadXML = new JMenuItem("LoadXML");
		menuitemFileLoadXML.addActionListener(m_controlChar);
		menuLoadAll.add(menuitemFileLoadXML);
		menuFile.add(menuLoadAll);
		
		JMenu menuSaveAll = new JMenu("Save");
		JMenuItem menuitemFileSaveData = new JMenuItem("SaveData");
		menuitemFileSaveData.addActionListener(m_controlChar);
		menuSaveAll.add(menuitemFileSaveData);
		JMenuItem menuitemFileSaveXML = new JMenuItem("SaveXML");
		menuitemFileSaveXML.addActionListener(m_controlChar);
		menuSaveAll.add(menuitemFileSaveXML);
		menuFile.add(menuSaveAll);
		
		menuFile.addSeparator();
		JMenuItem menuitemFileMerge = new JMenuItem("Merge");
		menuitemFileMerge.addActionListener(m_controlChar);
		menuFile.add(menuitemFileMerge);
		JMenuItem menuitemFileExport = new JMenuItem("Export");
		menuitemFileExport.addActionListener(m_controlChar);
		menuFile.add(menuitemFileExport);

		JMenu menuHelp = new JMenu("Help");
		JMenuItem menuitemHelpAbout = new JMenuItem("About");
		menuitemHelpAbout.addActionListener(m_controlChar);
		menuHelp.add(menuitemHelpAbout);
		JMenuItem menuitemHelpHTML = new JMenuItem("Help");
		menuitemHelpHTML.addActionListener(m_controlChar);
		menuHelp.add(menuitemHelpHTML);

		JMenuBar barReturn = new JMenuBar();
		barReturn.add(menuFile);
		barReturn.add(menuHelp);
		
		return barReturn;
	}
	
	@Override
	public boolean changeView(CharacterEventView eventView)
	{
		EventViewEnum enumView = eventView.getType();
		
		RegionData dataRegionContent;
		RegionData dataRegionParent;
		
		switch (enumView)
		{
		case _POPUP:
			dataRegionContent = eventView.getSource();
			dataRegionParent = (RegionData)dataRegionContent.getParent();
			if (null==dataRegionParent) return true;
			return popup(dataRegionContent, dataRegionParent);
			
		case _LAYOUT:
			dataRegionContent = eventView.getSource();
			CharacterEventLayout layoutEvent = (CharacterEventLayout) eventView;
			return layout(dataRegionContent, layoutEvent.isShowLayout());
			
		case _RESIZE:
		case _SHOW:
		default:
			break;		
		}
		return true;
	}
	
	@Override
	public boolean madeEdit(CharacterEventEdit eventCharacter) 
	{
		EventEditEnum enumEdit = eventCharacter.getType();
		
		if (eventCharacter instanceof CharacterEventLoaded) setResources(((CharacterEventLoaded) eventCharacter).getFileLoaded());
		
		RegionData dataRegionContent;
		RegionData dataRegionParent;
		
		switch (enumEdit)
		{
		case _ADD_SELECTION:
		case _REMOVE_SELECTION:
			// Handled at the level of the character view so do not do anything
			break;
						
		case _ADD_CATEGORY:
		case _REMOVE_CATEGORY:
			// Handled at the level of the region so do not do anything
			break;
			
		case _ADD_REGION:
		case _REMOVE_REGION:
			// Rebuild from the parent 
			dataRegionContent = eventCharacter.getSource();
			dataRegionParent = getSignificantParent(dataRegionContent);
			if (null==dataRegionParent) return true;
			return rebuild(this, dataRegionParent);
			
		case _REMOVE_ALL:
		case _UPDATE_ALL:
			return update();
			
		default:
			break;
		}
		
		return true;
	}
	
	private boolean rebuild(final CharacterView viewCharacter, final RegionData dataRegion)
	{
		Runnable runView = new Runnable()
		{
			@Override
			public void run() 
			{
				ViewHelper helperView = new ViewHelperRebuild(viewCharacter.m_dataCharacter, viewCharacter, viewCharacter.m_controlChar);
				// Find the component in the view
				JComponent componentParent = helperView.findComponent(m_containerChar, dataRegion);
				if (null==componentParent)
				{
					failMessage(new CharacterEventError(String.format("Could not rebuild %s view", dataRegion.getName())));
					return;
				}
				// Build the content of the modified view
				if (!dataRegion.handleView(helperView, componentParent))
				{
					failMessage(new CharacterEventError(String.format("Could not rebuild %s panel", dataRegion.getName())));
				}

				// Validate and repaint the changes
				
				componentParent.revalidate();
				componentParent.repaint();

				return;
			}
		};
		
		SwingUtilities.invokeLater(runView);
		
		return true;				
	}
	
	private RegionData getSignificantParent(RegionData dataRegionChild)
	{
		RegionData dataRegionParent = null;
		
		while (true)
		{
			dataRegionParent = (RegionData)dataRegionChild.getParent();
			if (null==dataRegionParent) return null;
			if (dataRegionParent instanceof RegionDataRoot) break;
			
			int iChildren = dataRegionParent.getChildCount();
			if (1<iChildren) break;
			
			dataRegionChild = dataRegionParent;
		}
		
		return dataRegionParent;
	}
	
	private boolean update()
	{
		Runnable runView = new Runnable()
		{
			@Override
			public void run() 
			{
				m_containerChar.removeAll();
				m_containerChar = buildPanel(m_controlChar, m_containerChar, m_dataCharacter);
				m_containerChar.revalidate();
				m_containerChar.repaint();
			}
			
		};
		SwingUtilities.invokeLater(runView);
		
		return true;		
	}
	
	private boolean popup(RegionData dataRegionContent, final RegionData dataRegionParent)
	{	
		Runnable runPopup = new Runnable()
		{
			@Override
			public void run() 
			{
				JComponent panelPopup = new JPanel();
				panelPopup.setLayout(new BorderLayout());
				if (!m_helperView.buildPanel(dataRegionParent, panelPopup)) 
				{
					dataRegionParent.fireCharacterEvent(new CharacterEventError("Could not create popup " + dataRegionParent.toString()));
					return;
				}
				ViewHelper.frameView(panelPopup, null, dataRegionParent.getName(), panelPopup.getPreferredSize());
			}
			
		};
		SwingUtilities.invokeLater(runPopup);
		
		return true;
	}
	
	private boolean layout(RegionData dataRegionContent, final boolean zShowLayout)
	{	
		Runnable runLayout = new Runnable()
		{
			@Override
			public void run() 
			{
				if (null==m_nodeBorder)
				{
					Border borderHighlight = BorderFactory.createLineBorder(Color.ORANGE, 3);
					m_nodeBorder = new DefaultMutableTreeNode();
					addBorder(m_nodeBorder, m_containerChar, borderHighlight);
				}
				else
				{
					removeBorder(m_nodeBorder, m_containerChar);
					m_nodeBorder = null;					
				}
				m_containerChar.repaint();
				return;
			}
			
		};
		SwingUtilities.invokeLater(runLayout);
		
		return true;
	}
	
	private boolean addBorder(DefaultMutableTreeNode nodeBorder, JComponent container, Border border)
	{
		Component[] aContent = container.getComponents();
		for (Component component : aContent)
		{
			if (!(component instanceof JComponent)) continue;
			JComponent content = (JComponent)component;
			
			if (content instanceof CategoryView)
			{
				border = BorderFactory.createLineBorder(Color.MAGENTA, 3);
			}
			
			if (content instanceof SelectionView)
			{
				border = BorderFactory.createLineBorder(Color.CYAN, 2);
			}
			
			DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode();
			nodeBorder.add(nodeChild);
			
			try
			{
				// Set the border and record in tree
				Border borderNow = content.getBorder();
				content.setBorder(border);
				nodeChild.setUserObject(borderNow);				
			}
			catch (IllegalArgumentException x)
			{
				// Some components do not support a border
			}
			
			// Iterate
			addBorder(nodeChild, content, border);
		}
		
		return true;
	}
	
	private boolean removeBorder(DefaultMutableTreeNode nodeBorder, JComponent container)
	{
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> enumChildren = nodeBorder.children();
		
		Component[] aContent = container.getComponents();
		for (Component component : aContent)
		{
			if (!(component instanceof JComponent)) continue;
			JComponent content = (JComponent)component;
			DefaultMutableTreeNode nodeChild = enumChildren.nextElement();
			
			Border borderChild = (Border)nodeChild.getUserObject();
			
			try
			{
				// Reset the border
				content.setBorder(borderChild);
			}
			catch (IllegalArgumentException x)
			{
				// Some components do not support a border
			}
			
			// Iterate
			removeBorder(nodeChild, content);
		}
		
		return true;
	}
	
	@Override
	public boolean failMessage(CharacterEventError eventCharacter) 
	{
		JOptionPane.showMessageDialog(m_containerChar, eventCharacter.getError(), "CharacterView", JOptionPane.ERROR_MESSAGE);
		return true;
	}
	
	
	@Override
	public Dimension getInitialSize() 
	{
		return new Dimension(500, 600);
	}
	
	@Override
	public Color getColour(OptionsColourEnum enumColour) 
	{
		switch (enumColour)
		{
		case _SELECTED:			
			return sm_acoloursSet[0]; // selected
		case _EXCLUDED:
			return sm_acoloursSet[1]; // excluded
		case _SUGGESTED:
			return sm_acoloursSet[2]; // suggested
		case _WARNING:
			return sm_acoloursSet[3]; // normal
		case _ERROR:
			return sm_acoloursSet[4]; // warning
		case _BACK:
			return sm_acoloursSet[5]; // back
		case _TEXT:
			return sm_acoloursSet[6];
		}
		return Color.BLACK;
	}
	
	private static Color sm_colourSelected = new Color(0x99FF99);
	private static Color sm_colourExcluded = new Color(0xFF9966);
	private static Color sm_colourSuggested = new Color(0x99CCFF);
	private static Color sm_colourNormal = new Color(0xFFFF66);
	private static Color sm_colourWarning = new Color(0xFF3300);
	private static Color sm_colourBack = new Color(0xFFFCDE);
	private static Color sm_colourText = new Color(0x060606);
	
	private static Color[] sm_acoloursSet = {sm_colourSelected, sm_colourExcluded, sm_colourSuggested, sm_colourNormal, sm_colourWarning, sm_colourBack, sm_colourText};

	public void setResources(File fileLoaded)
	{
		if (null==fileLoaded)
		{
			//URL urlClass = CharacterView.class.getClassLoader().getResource(".");
			// TODO get resources from default
			return;
		}
		m_pathResources = fileLoaded.toPath();
		m_pathResources = m_pathResources.getParent();
	}
	
	@Override
	public Path getResources() 
	{
		return m_pathResources;
	}

	@Override
	public Font getFont(Font fontSource, OptionsFontEnum enumPurpose) 
	{
		int iStyle = Font.PLAIN, iSize = 14;
		
		switch (enumPurpose)
		{
		case _CHARACTER:
			iStyle = Font.BOLD;
			iSize = 18;
			break;
		case _TAB:
			break;
		case _CATEGORY:
			break;
		case _SELECTION:
			break;
		case _TEXT:
			iStyle = Font.BOLD;
			break;
		default:
			break;
		}
		
		return fontSource.deriveFont(iStyle, iSize);
	}

	@Override
	public Font getFont(JComponent componentSource, OptionsFontEnum enumPurpose) 
	{
		Font fontSource = componentSource.getFont();
		return getFont(fontSource, enumPurpose);
	}
}

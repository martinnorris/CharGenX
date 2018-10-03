package generator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

enum ViewHelperTypes {_DEFAULT, _LIST, _COMBO, _TEXT, _BACKGROUND, _GRAPHIC, _SIZE};

class ViewHelper
{
	ViewHelper(CharacterData dataCharacter, Options options, ActionListener listenerAction)
	{
		m_dataCharacter = dataCharacter;
		m_options = options;
		m_listenerAction = listenerAction;
		m_aViewHelpers = new ViewHelper[ViewHelperTypes._SIZE.ordinal()];
	}
	
	private CharacterData m_dataCharacter = null;
	
	protected Options m_options = null;
	protected ActionListener m_listenerAction = null;
	
	// Set when first createPanel is invoked
	private ViewHelper[] m_aViewHelpers = null;
			
	public static boolean framePopup(final Component componentParent, final JPanel panelContent, final String scFrameTitle)
	{
		Runnable startFrame = new Runnable()
		{
			@Override
			public void run()
			{			
				Window windowApp = null;
				if (null!=componentParent) windowApp = SwingUtilities.getWindowAncestor(componentParent);
				
				JDialog dialogPopup = new JDialog(windowApp);
				dialogPopup.setTitle(scFrameTitle);
				dialogPopup.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialogPopup.setContentPane(panelContent);
				dialogPopup.pack();
				dialogPopup.setVisible(true);
			}
		};
		
		SwingUtilities.invokeLater(startFrame);
		
		return true;		
	}
	
	public static void frameView(final Component componentPanel, final JMenuBar barMenu, final String scFrameTitle, final Dimension dimensionSize)
	{
		Runnable startFrame = new Runnable()
		{
			@Override
			public void run()
			{
				JFrame frameChar = new JFrame(scFrameTitle);
				frameChar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				
				frameChar.add(componentPanel);
				if (null!=barMenu) frameChar.setJMenuBar(barMenu);
				if (null!=barMenu) frameChar.addWindowListener(new WindowAdapterExit(barMenu));
				frameChar.pack();
				
				Dimension dimExpected = frameChar.getMinimumSize();
				int iHeight = (int)Math.max(dimExpected.getHeight(), dimensionSize.getHeight()); // (int) (dimExpected.getHeight() * 400 / dimExpected.getWidth());
				
				Dimension dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
				int iWidth = (int)Math.max(dimExpected.getWidth(), dimensionSize.getWidth()); // (int) (dimExpected.getWidth() * 800 / dimExpected.getHeight());
	
				if (200>dimExpected.getWidth()) dimExpected = new Dimension((int)dimensionSize.getWidth(), iHeight);
				if (800<dimExpected.getHeight()) dimExpected = new Dimension(iWidth, (int)dimensionSize.getHeight());
	
				frameChar.setMinimumSize(dimExpected);
				frameChar.setMaximumSize(dimScreen);
				
				int iX = (int)(dimScreen.getWidth() - dimExpected.getWidth()) / 2 ;
				int iY = (int)(dimScreen.getHeight() - dimExpected.getWidth()) / 3 ;
				
				frameChar.setLocation(new Point(iX, iY));
				frameChar.setVisible(true);
			}
		};
		
		SwingUtilities.invokeLater(startFrame);

		return;
	}
	
	protected ViewHelper getViewHelper(ViewHelperTypes typeViewHelper)
	{
		int iIndex = typeViewHelper.ordinal();
		
		if (null==m_aViewHelpers[iIndex])
		{
			ViewHelper helperCreated = null;
			
			switch (typeViewHelper)
			{
			case _DEFAULT:
				helperCreated = new ViewHelperDefault(m_dataCharacter, m_options, m_listenerAction);
				break;
			case _LIST:
				helperCreated = new ViewHelperList(m_dataCharacter, m_options, m_listenerAction);
				break;
			case _COMBO:
				helperCreated = new ViewHelperCombo(m_dataCharacter, m_options, m_listenerAction);
				break;
			case _TEXT:
				helperCreated = new ViewHelperText(m_dataCharacter, m_options, m_listenerAction);
				break;
			case _BACKGROUND:
				helperCreated = new ViewHelperBackground(m_dataCharacter, m_options, m_listenerAction);
				break;
			case _GRAPHIC:
				helperCreated = new ViewHelperGraphic(m_dataCharacter, m_options, m_listenerAction, this);
				break;
				
			default:
				return this;
			}
			m_aViewHelpers[iIndex] = helperCreated;
		}
		
		return m_aViewHelpers[iIndex];
	}
	
	private ViewHelper getViewHelper(String scView)
	{
		int iIndex = scView.indexOf('_');
		if (0>iIndex) return this;
		
		// View starts with '_' so adjust the view shown
		
		String scHint = scView.substring(iIndex+1);
		
		if (scHint.startsWith("panel")) return getViewHelper(ViewHelperTypes._DEFAULT);
		if (scHint.startsWith("list")) return getViewHelper(ViewHelperTypes._LIST);
		if (scHint.startsWith("combo")) return getViewHelper(ViewHelperTypes._COMBO);
		if (scHint.startsWith("text")) return getViewHelper(ViewHelperTypes._TEXT);		
		if (scHint.startsWith("background")) return getViewHelper(ViewHelperTypes._BACKGROUND);
		if (scHint.startsWith("graphic")) return getViewHelper(ViewHelperTypes._GRAPHIC);
		
		return this;
	}
	
	// Create panel uses a friend pattern to callback from data to view to create
	public JComponent createPanel(JComponent componentContainer)
	{
		// Entry from CharacterView with ViewHelperList
		if (!m_dataCharacter.handleView(this, componentContainer)) throw new RuntimeException(String.format("Could not build panel for %s", m_dataCharacter));
		return componentContainer;
	}
	
	public JComponent findComponent(JComponent componentView, RegionData dataRegionFind) 
	{
		ViewHelper helperView = getViewHelper(ViewHelperTypes._LIST);
		return helperView.findComponentCharacter(1, m_dataCharacter, componentView, dataRegionFind);
	}
	
	public SelectionData findSelectionData(String scReference)
	{
		EvaluationHelper helperEvaluation = m_dataCharacter.getEvaluation();
		return helperEvaluation.referenceSelection(scReference);
	}
	
	// CharacterData - panel built from character generator name so can have different types of character presented
	
	// TODO different characters
	
	public boolean buildPanel(CharacterData dataCharacter, JComponent componentView)
	{
		String scView = dataCharacter.toString();
		ViewHelper helperView = getViewHelper(scView);
		return helperView.buildView(dataCharacter, componentView);
	}
	
	// Override to build character view
	protected boolean buildView(CharacterData dataCharacter, JComponent componentContainer)
	{
		return false;
	}
	
	// Override to build character view content
	protected boolean buildContent(CharacterData dataCharacter, JComponent componentCategory)
	{
		return false;
	}
	
	// Override to find from character level
	protected JComponent findComponentCharacter(int iLevel, CharacterData dataCharacter, JComponent componentView, RegionData dataRegionFind)
	{
		// Finding the region depends on the view 
		String scView = dataCharacter.toString();
		ViewHelper helperView = getViewHelper(scView);
		// No change in view
		if (this==helperView) return null;
		return helperView.findComponentCharacter(iLevel, dataCharacter, componentView, dataRegionFind);
	}
	
	// RegionData - panel built from region name so can have different categories organized different ways
	
	public boolean buildPanel(RegionData dataRegion, JComponent componentView)
	{
		String scView = dataRegion.getName();
		ViewHelper helperView = getViewHelper(scView);
		return helperView.buildView(dataRegion, componentView);		
	}
	
	// Override to build region view
	protected boolean buildView(RegionData dataCategory, JComponent componentContainer)
	{
		return false;
	}
	
	// Override to build region view content
	protected boolean buildContent(RegionData dataCategory, JComponent componentCategory)
	{
		return false;
	}
	
	protected JComponent findComponentRegion(int iLevel, RegionData dataRegion, JComponent componentView, RegionData dataRegionFind)
	{
		String scView = dataRegion.getName();
		ViewHelper helperView = getViewHelper(scView);
		// If the getViewHelper returns ViewHelper then this goes into a loop
		return helperView.findComponentRegion(iLevel, dataRegion, componentView, dataRegionFind);
	}
	
	// CategoryData - panel built from category name so for types like panel or list (default) or combo which changes the helper
	
	// This is the entry point for a CategoryData building the view
	public boolean buildPanel(CategoryData dataCategory, JComponent componentContainer)
	{
		String scView = dataCategory.getName();
		ViewHelper helperView = getViewHelper(scView);
		return helperView.buildView(dataCategory, componentContainer);
	}
	
	// Override to build category view
	protected boolean buildView(CategoryData dataCategory, JComponent componentContainer)
	{
		return false;
	}
	
	// Override to build category view content
	protected boolean buildContent(CategoryData dataCategory, CategoryView viewCategory, JComponent componentCategory)
	{
		return false;
	}
	
	protected JComponent findComponentCategory(int iLevel, CategoryData dataCategory, JComponent componentView, RegionData dataRegionFind) 
	{
		if (dataCategory.sameCategory(dataRegionFind)) return componentView;
		return null;
	}
	
	// SelectionData - panel built from selection view {only changed for graphic panel}
	
	public boolean buildPanel(SelectionData dataSelection, CategoryView viewCategory, JComponent componentCategory)
	{
		// When selection is append _graphic then create graphic selection {icon in label so should work in panel list and combo}
		String scView = dataSelection.getName();
		ViewHelper helperView = getViewHelper(scView);
		// No other hints in the name
		return helperView.buildView(dataSelection, componentCategory);
	}
	
	protected boolean buildView(SelectionData dataSelection, JComponent componentCategory)
	{
		return false;
	}
	
	protected boolean buildContent(SelectionData dataSelection, SelectionView viewSelection, JComponent componentContainer, String scHint)
	{
		return false;
	}
	
	// SelectionDataProxy - panel built from selection proxy view but probably not useful to change the helper
	
	public boolean buildPanel(SelectionDataProxy dataSelectionProxy, CategoryView viewCategory, JComponent componentContainer)
	{
		//String scView = dataSelectionProxy.getView();
		String scName = dataSelectionProxy.getName();
		int iIndex = scName.indexOf('_');
		if (0>iIndex) iIndex = scName.length();
		String scView = scName.substring(iIndex, scName.length());
		// Remove multiple '_' at beginning
		for (; '_'==scView.charAt(1); scView = scView.substring(1));
		
		return buildView(dataSelectionProxy, viewCategory, componentContainer, scView);		
	}
	
	protected boolean buildView(SelectionDataProxy dataSelection, CategoryView viewCategory, JComponent componentContainer, String scView)
	{
		return false;
	}
	
	protected boolean buildContent(SelectionDataProxy dataSelectionProxy, SelectionView viewSelection, CategoryView viewCategory, JComponent componentContainer, String scHint)
	{
		return false;
	}
}

class WindowAdapterExit extends WindowAdapter
{
	public WindowAdapterExit(JMenuBar barMenu)
	{
		m_barMenu = barMenu;
	}
	
	private JMenuBar m_barMenu = null;
	
	@Override
	public void windowClosed(WindowEvent arg0) 
	{
		JMenu menuFile = m_barMenu.getMenu(0);
		JMenuItem entryExit = menuFile.getItem(0);
		entryExit.doClick();
		return;
	}		
}	

class ViewHelperDefault extends ViewHelper
{
	ViewHelperDefault(CharacterData dataCharacter, Options options, ActionListener listenerAction)
	{
		super(dataCharacter, options, listenerAction);
	}
	
	// CharacterData - build view as tabbed pane and then build content of regions
	
	class CharacterTabbedPane extends JTabbedPane
	{
		private static final long serialVersionUID = -3181130234414858766L;

		CharacterTabbedPane(String scName)
		{
			m_scName = scName;
		}
		
		private String m_scName = null;
		
		@Override
		public String toString()
		{
			return m_scName;
		}
	}
	
	@Override
	protected boolean buildView(CharacterData dataCharacter, JComponent componentContainer)
	{
		RegionData dataRoot = dataCharacter.getRoot();
		Iterator<RegionData> iterateCategory = dataRoot.getRegions();

		if (!iterateCategory.hasNext())
		{
			JLabel labelDefault = new JLabel("No document structure");
			labelDefault.setHorizontalAlignment(JLabel.CENTER);
			componentContainer.add(labelDefault);
			return true;
		}
		
		JTabbedPane paneCharacter = new CharacterTabbedPane(String.format("Character %s tabbed pane", dataCharacter.toString()));
		// paneCharacter.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 3));
		
		if (!buildContent(dataCharacter, paneCharacter)) throw new RuntimeException(String.format("Could not build view %s", dataCharacter));		
		componentContainer.add(paneCharacter);
		
		return true;
	}
	
	class CharacterPanel extends JPanel
	{
		private static final long serialVersionUID = 5448500509966684010L;

		CharacterPanel(String scName)
		{
			m_scName = scName;
		}
		
		private String m_scName = null;
		
		@Override
		public String toString()
		{
			return m_scName;
		}		
	}
	
	@Override
	protected boolean buildContent(CharacterData dataCharacter, JComponent componentCategory)
	{
		RegionDataRoot dataRoot = (RegionDataRoot)dataCharacter.getRoot();
		
		// Get regions order they were created
		Iterator<RegionData> iterateCategory = dataRoot.getUnsortedRegions();
		JTabbedPane paneCharacter = (JTabbedPane)componentCategory;

		while (iterateCategory.hasNext())
		{
			RegionData dataRegion = iterateCategory.next();
			
			JPanel panelPane = new CharacterPanel(String.format("Character/Region %s panel", dataRegion.toString()));
			panelPane.setLayout(new BorderLayout());
						
			if (!dataRegion.handleView(this, panelPane)) throw new RuntimeException(String.format("Could not build content for %s", dataCharacter));
			
			paneCharacter.addTab(dataRegion.getName(), panelPane);
		}
		
		return true;
	}

	@Override
	protected JComponent findComponentCharacter(int iLevel, CharacterData dataCharacter, JComponent componentView, RegionData dataRegionFind) 
	{
		// A character is built from a number of regions
		RegionDataRoot dataRoot = (RegionDataRoot)dataCharacter.getRoot();
		Iterator<RegionData> iterateRegion = dataRoot.getUnsortedRegions();
		
		// If there is no region data for character return this region
		if (!iterateRegion.hasNext()) return componentView;
		
		// Panel for character built of panels in tabbed panes so check through for region
		JTabbedPane paneCharacter = (JTabbedPane)componentView.getComponent(0);
		Component[] aComponents = paneCharacter.getComponents();
		
		for (int iIndex = 0; iIndex<aComponents.length; ++iIndex)
		{
			// A character view is built of JPanels in JTabbedPanes
			JPanel panelRegion = (JPanel)aComponents[iIndex];
			
			// Must keep region and component in sync so if no region (has been deleted return parent)
			if (!iterateRegion.hasNext()) return componentView;
			RegionData dataRegionNext = iterateRegion.next();
			
			if (dataRegionFind==dataRegionNext) return panelRegion;
			
			// Search the component for the region - go to super so view helper can be checked
			componentView = super.findComponentRegion(iLevel+1, dataRegionNext, panelRegion, dataRegionFind);
			if (null==componentView) continue;
			
			return componentView;
		}
				
		return null;
	}
	
	// RegionData - build view as panel and then build content of categories in vertical box layout
	
	class RegionPanel extends JPanel
	{
		private static final long serialVersionUID = -7627953263026342746L;

		RegionPanel(String scName)
		{
			m_scName = scName;
		}
		
		private String m_scName = null;
		
		@Override
		public String toString()
		{
			return m_scName;
		}				
	}
	
	class ViewHelperArguments extends ParseArgument
	{
		ViewHelperArguments(String scArguments)
		{
			super(scArguments);
		}
		
		private Map<String, Object> m_mapArguments = null;
		
		private Map<String, Object> createArguments()
		{
			Map<String, Object> mapArguments = new HashMap<String, Object>();
			
			// Find separator '_'
			while (hasSymbol() && '_'!=nextChar()) consume(1);
			// No arguments
			if ('_'!=nextChar()) return mapArguments;
			
			// First comes the type
			String scType = getStringRegex();
			mapArguments.put("TYPE", scType);
			 
			String scKey = getStringRegex();
			if (EvaluationHelper.END_PARSING==nextChar()) return mapArguments;
				
			String scValue = getStringRegex();
			
			if (scKey.startsWith("AXIS"))
			{
				if (scValue.startsWith("V")) mapArguments.put(scKey, new Integer(BoxLayout.PAGE_AXIS));
				if (scValue.startsWith("H")) mapArguments.put(scKey, new Integer(BoxLayout.LINE_AXIS));
				mapArguments.put("GLUE", Box.createVerticalGlue());
			}
			
			return mapArguments;
		}
		
		public int getArgument(String scKey, int iDefault)
		{
			if (null==m_mapArguments) m_mapArguments = createArguments();
			if (!m_mapArguments.containsKey(scKey)) return iDefault;
			Integer valueInteger = (Integer)m_mapArguments.get(scKey);
			int iAxis = valueInteger.intValue();
			
			if (BoxLayout.PAGE_AXIS==iAxis)
				m_mapArguments.put("GLUE", Box.createVerticalGlue());
			else
				m_mapArguments.put("GLUE", Box.createHorizontalGlue());
				
			return iAxis;
		}
		
		public Component getArgument(String scKey, Component componentDefault)
		{
			if (null==m_mapArguments) m_mapArguments = createArguments();
			if (!m_mapArguments.containsKey(scKey)) return componentDefault;
			return (Component) m_mapArguments.get(scKey);
		}
	}
	
	@Override
	protected boolean buildView(RegionData dataRegion, JComponent componentContainer)
	{
		if (1==dataRegion.getChildCount()) return buildContentSingle(dataRegion, componentContainer);
		
		// Extract arguments for panel built
		String scView = dataRegion.getName();
		ViewHelperArguments argumentsRegion = new ViewHelperArguments(scView);
		
		int iPageAxis = argumentsRegion.getArgument("AXIS", BoxLayout.PAGE_AXIS);
		Component componentGlue = argumentsRegion.getArgument("GLUE", Box.createVerticalGlue());
		
		return buildViewRegion(dataRegion, componentContainer, iPageAxis, componentGlue);
	}
	
	private boolean buildContentSingle(RegionData dataRegion, JComponent componentContainer)
	{
		Iterator<RegionData> iterateRegions = dataRegion.getRegions();
		RegionData dataSubRegion = iterateRegions.next();
		if (!dataSubRegion.handleView(this, componentContainer)) throw new RuntimeException(String.format("Could not build content for %s", dataRegion));
		
		return true;	
	}

	private boolean buildViewRegion(RegionData dataRegion, JComponent componentContainer, int iAxis, Component componentGlue)
	{
		// View for region is a panel with NO border in a scroll pane
		JPanel panelContent = new RegionPanel(String.format("Region %s panel", dataRegion.toString()));
		panelContent.setLayout(new BoxLayout(panelContent, iAxis));
		if (!buildContent(dataRegion, panelContent)) return false;
		panelContent.add(componentGlue);

		JScrollPane paneRegion = new JScrollPane(panelContent);
		componentContainer.add(paneRegion);

		return true;
	}
	
	@Override
	protected boolean buildContent(RegionData dataRegion, JComponent componentContainer)
	{
		// Content is added to the panel with a separator
		
		Iterator<RegionData> iterateRegions = dataRegion.getRegions();
		
		for (boolean zAddSpacing = false; iterateRegions.hasNext(); zAddSpacing = true)
		{
			if (zAddSpacing) componentContainer.add(Box.createRigidArea(new Dimension(5, 5)));
			RegionData dataSubRegion = iterateRegions.next();
			if (!dataSubRegion.handleView(this, componentContainer)) throw new RuntimeException(String.format("Could not build content for %s", dataRegion));
		}
		
		return true;
	}
	
	// Need to find a particular region in the view from the root
	@Override
	protected JComponent findComponentRegion(int iLevel, RegionData dataRegion, JComponent componentView, RegionData dataRegionFind) 
	{
		Iterator<RegionData> iterateRegion = dataRegion.getRegions();
		// If there is no region data return parent
		if (!iterateRegion.hasNext()) return componentView;		

		if (1==dataRegion.getChildCount())
		{
			// If the single region is the searched for region then must have found the container
			RegionData dataRegionSingle = iterateRegion.next();
			if (dataRegionFind==dataRegionSingle) return componentView;

			// Region has single category
			if (dataRegionSingle instanceof CategoryData) return super.findComponentCategory(iLevel+1, (CategoryData)dataRegionSingle, componentView, dataRegionFind);

			// A single region is ignored for view construction so try the children
			return super.findComponentRegion(iLevel+1, dataRegionSingle, componentView, dataRegionFind);
		}
		
		// A region is built into a panel in a scroll window (2 levels of components - the scroll pane and the viewport)
		JScrollPane paneScroll = (JScrollPane) componentView.getComponent(0);
		JViewport viewScroll = (JViewport) paneScroll.getComponent(0);
		JPanel panelScroll = (JPanel) viewScroll.getComponent(0);
		Component[] aComponents = panelScroll.getComponents();
				
		// Each region is a panel separated by a spacing box so skip every other component 
		for (int iIndex = 0; iIndex<aComponents.length; iIndex+=2)
		{
			JComponent componentRegion = (JComponent) aComponents[iIndex];
			// Must keep region and component in sync
			if (!iterateRegion.hasNext()) return componentView;
			
			// Maybe found the region
			RegionData dataRegionNext = iterateRegion.next();
			if (dataRegionFind==dataRegionNext) return panelScroll;
			
			// Only searching for region or category
			if (dataRegionNext instanceof CategoryData) return findComponentRegion(iLevel+1, (CategoryData)dataRegionNext, componentView, dataRegionFind);

			componentRegion = findComponentRegion(iLevel+1, dataRegionNext, componentRegion, dataRegionFind);
			if (null==componentRegion) continue;
			
			return componentRegion;			
		}
				
		return null;
	}
	
	// CategoryData - build view as CategoryView and then build content of selections as CategoryView can handle (the helper will have been changed different types)
	
	@Override
	protected boolean buildView(CategoryData dataCategory, JComponent componentContainer)
	{
		CategoryView viewCategory = new CategoryViewPanel(this, dataCategory, m_options);
		JComponent componentCategory = viewCategory.buildPanel(m_listenerAction);
		componentCategory.setAlignmentY(0);
		componentContainer.add(componentCategory);
		return true;
	}
	
	@Override
	protected boolean buildContent(CategoryData dataCategory, CategoryView viewCategory, JComponent componentCategory)
	{
		// The content of the panel is inserted and also entry for updating content of category
		
		Iterator<RegionData> iterateSelection = dataCategory.getRegions(); // Add content same order as in category
		
		while (iterateSelection.hasNext())
		{
			SelectionData dataSelection = (SelectionData)iterateSelection.next();
			if (!dataSelection.handleView(this, componentCategory)) throw new RuntimeException(String.format("Could not build content for %s", dataCategory));
		}
		
		return true;		
	}
	
	// SelectionData - build view as SelectionView and then build content of selections as SelectionView can handle
	
	@Override
	protected boolean buildView(SelectionData dataSelection, JComponent componentCategory)
	{
		// Default is to use the value view because if the SelectionData has no value it defaults to the name {most of time the data is a simple selection which does not have a value}
		SelectionView viewSelection = new SelectionViewValue();
		return buildContent(dataSelection, viewSelection, componentCategory, "");
	}

	@Override
	protected boolean buildContent(SelectionData dataSelection, SelectionView viewSelection, JComponent componentContainer, String scHint)
	{
		// The selection proxy will invoke this routine with a hint
		JComponent componentView = viewSelection.buildPanel(dataSelection, m_options, scHint);
		scHint = viewSelection.applyHints(dataSelection, scHint);
		// TODO layout hints unset
		componentContainer.add(componentView);
		return true;		
	}
	
	// SelectionDataProxy - build view as SelectionViewMultiple and then build content from view for SelectionData from proxy source
	
	@Override
	protected boolean buildView(SelectionDataProxy dataSelectionProxy, CategoryView viewCategory, JComponent componentCategory, String scView)
	{
		// Build a panel using the default view helper
		SelectionViewMultiple viewSelection = new SelectionViewMultiple(this, m_listenerAction);
		SelectionData dataSelection = dataSelectionProxy.getSource();
		// Build content from view for SelectionData from proxy source
		return buildContent(dataSelection, viewSelection, componentCategory, scView);
	}	
}

class ViewHelperList extends ViewHelperDefault
{
	ViewHelperList(CharacterData dataCharacter, Options options, ActionListener listenerAction)
	{
		super(dataCharacter, options, listenerAction);
		m_borderSelection = BorderFactory.createEmptyBorder(3,3,3,3);
	}
	
	private Border m_borderSelection = null;
	
	// CategoryData - build view as list and then build content _indirectly_ from the CategoryView calls back to iterates for the content
	
	@Override
	protected boolean buildView(CategoryData dataCategory, JComponent componentContainer)
	{
		// The buildPanel uses buildView to create the correct type of view
		CategoryViewList viewCategory = new CategoryViewList(this, dataCategory, m_options);
		JComponent componentCategory = viewCategory.buildPanel(m_listenerAction);
		componentContainer.add(componentCategory);		
		return true;
	}
	
	// SelectionData - build view as default and add content to category view with a empty space border
	
	@Override
	protected boolean buildContent(SelectionData dataSelection, SelectionView viewSelection, JComponent componentContainer, String scHint)
	{
		SelectionViewBase labelSelection = (SelectionViewBase)viewSelection.buildPanel(dataSelection, m_options, scHint);
		labelSelection.setBorder(m_borderSelection);
		labelSelection.setOpaque(true);
		
		CategoryViewListComponent listCategory = (CategoryViewListComponent)componentContainer;
		if (labelSelection instanceof SelectionViewGraphic) listCategory.setHeightHint(CategoryViewListComponent.HEIGHT_FOR_GRAPHIC);
		
		DefaultListModel<SelectionViewBase> listSelection = (DefaultListModel<SelectionViewBase>)listCategory.getModel();
		listSelection.addElement(labelSelection);
		
		return true;		
	}
	
	// SelectionDataProxy - cannot add a panel to a list but OK for labels

	@Override
	protected boolean buildView(SelectionDataProxy dataSelectionProxy, CategoryView viewCategory, JComponent componentCategory, String scView)
	{
		SelectionViewMultiple viewSelection = new SelectionViewMultipleLabel(this, m_listenerAction);
		SelectionData dataSelection = dataSelectionProxy.getSource();
		// Build content from view for SelectionData from proxy source
		return buildContent(dataSelection, viewSelection, componentCategory, scView);
	}
}

class ViewHelperCombo extends ViewHelperDefault
{
	ViewHelperCombo(CharacterData dataCharacter, Options options, ActionListener listenerAction) 
	{
		super(dataCharacter, options, listenerAction);
	}
	
	private Border m_borderSelection = null;

	// CategoryData - build view as combo and then build content _indirectly_ from the CategoryView calls back to iterates for the content
	
	@Override
	protected boolean buildView(CategoryData dataCategory, JComponent componentContainer)
	{
		m_borderSelection = BorderFactory.createEmptyBorder(3,3,3,3);
		CategoryViewCombo viewCategory = new CategoryViewCombo(this, dataCategory, m_options);
		JComponent componentCategory = viewCategory.buildPanel(m_listenerAction);
		componentContainer.add(componentCategory);
		return true;
	}
	
	// SelectionData - build view as default and add content to category view with a empty space border
	
	@Override
	protected boolean buildContent(SelectionData dataSelection, SelectionView viewSelection, JComponent componentContainer, String scHint)
	{
		SelectionViewBase labelSelection = (SelectionViewBase)viewSelection.buildPanel(dataSelection, m_options, scHint);
		labelSelection.setBorder(m_borderSelection);
		labelSelection.setOpaque(true);
		//labelSelection.setBackground(Color.MAGENTA.brighter());
		
		CategoryViewComboComponent comboCategory = (CategoryViewComboComponent)componentContainer;
		comboCategory.addItem(labelSelection);

		return true;
	}
	
	// SelectionDataProxy - cannot add a panel to a combo but OK for labels

	@Override
	protected boolean buildView(SelectionDataProxy dataSelectionProxy, CategoryView viewCategory, JComponent componentContainer, String scView)
	{
		// Build a panel using the default view helper
		SelectionViewMultiple viewSelection = new SelectionViewMultipleLabel(this, m_listenerAction);
		SelectionData dataSelection = dataSelectionProxy.getSource();
		// Build content from view for SelectionData from proxy source
		return buildContent(dataSelection, viewSelection, componentContainer, scView);
	}	
}

class ViewHelperText extends ViewHelperDefault
{
	ViewHelperText(CharacterData dataCharacter, Options options, ActionListener listenerAction) 
	{
		super(dataCharacter, options, listenerAction);
	}
	
	// CategoryData - build view as panel and then build content _indirectly_ from the CategoryView calls back to iterates for the content with box layout
	
	@Override
	protected boolean buildView(CategoryData dataCategory, JComponent componentContainer)
	{
		// Minimum panel to show options
		CategoryView viewCategory = new CategoryViewPanel(this, dataCategory, m_options);
		JComponent componentCategory = viewCategory.buildPanel(m_listenerAction);
		componentCategory.setLayout(new BoxLayout(componentCategory, BoxLayout.PAGE_AXIS));
		componentContainer.add(componentCategory);
		return true;
	}
	
	// SelectionData - build view as field and add to panel as default
	
	@Override
	protected boolean buildView(SelectionData dataSelection, JComponent componentCategory)
	{
		SelectionViewField viewSelection = new SelectionViewField(m_listenerAction);
		return buildContent(dataSelection, viewSelection, componentCategory, "");
	}
}

class ViewHelperBackground extends ViewHelperDefault
{
	ViewHelperBackground(CharacterData dataCharacter, Options options, ActionListener listenerAction) 
	{
		super(dataCharacter, options, listenerAction);
	}
	
	// Region data - background region does not place sub region in scroll pane
	
	@Override
	protected JComponent findComponentRegion(int iLevel, RegionData dataRegion, JComponent componentView, RegionData dataRegionFind) 
	{
		Iterator<RegionData> iterateRegion = dataRegion.getRegions();
		// If there is no region data return parent
		if (!iterateRegion.hasNext()) return componentView;

		// A region is built into a panel in a scroll window (2 levels of components - the scroll pane and the viewport)
		JPanel panelBackground = (JPanel) componentView.getComponent(0);
		Component[] aComponents = panelBackground.getComponents();
				
		// Each region is a panel separated by a spacing box so skip every other component 
		for (int iIndex = 0; iIndex<aComponents.length; iIndex+=2)
		{
			JComponent componentRegion = (JComponent) aComponents[iIndex];
			// Must keep region and component in sync
			if (!iterateRegion.hasNext()) return componentView;
			
			// Maybe found the region
			RegionData dataRegionNext = iterateRegion.next();
			if (dataRegionFind==dataRegionNext) return panelBackground;
			
			// Only searching for region or category
			if (dataRegionNext instanceof CategoryData) return super.findComponentRegion(iLevel+1, (CategoryData)dataRegionNext, componentView, dataRegionFind);

			componentRegion = super.findComponentRegion(iLevel+1, dataRegionNext, componentRegion, dataRegionFind);
			if (null==componentRegion) continue;
			
			return componentRegion;			
		}
				
		return null;
	}
	
	// CategoryData - build category view with a picture in background and content built by iteration
	
	@Override
	protected boolean buildView(CategoryData dataCategory, JComponent componentContainer)
	{
		CategoryViewBackground viewCategory = new CategoryViewBackground(this, dataCategory, m_options);
		JComponent componentCategory = viewCategory.buildPanel(m_listenerAction);
		componentContainer.add(componentCategory);
		return true;
	}
	
	// SelectionData - build selection view as value {always} and when building content force a hint {if it does not exist}
	
	@Override
	public boolean buildPanel(SelectionData dataSelection, CategoryView viewCategory, JComponent componentCategory)
	{
		// Need to override default since default forces a helper change to graphic if the selection is _graphic
		return buildView(dataSelection, componentCategory);
	}
	
	// TODO can use any selection view
	
	@Override
	protected boolean buildView(SelectionData dataSelection, JComponent componentCategory)
	{
		// When added to background always add label panel
		SelectionView viewSelection = new SelectionViewValue();
		return buildContent(dataSelection, viewSelection, componentCategory, "");
	}	
	
	private static int sm_iY = 0;
	
	protected boolean buildContent(SelectionData dataSelection, SelectionView viewSelection, JComponent componentContainer, String scHints)
	{
		JComponent componentSelection = viewSelection.buildPanel(dataSelection, m_options, scHints);
		scHints = viewSelection.applyHints(dataSelection, scHints);

		// Must have a hint for position on background
		if (0==scHints.length()) scHints = String.format("5,%d", 5+8*sm_iY++);		
		componentContainer.add(componentSelection, scHints);
		
		return true;
	}
}

class ViewHelperGraphic extends ViewHelperList
{
	ViewHelperGraphic(CharacterData dataCharacter, Options options, ActionListener listenerAction, ViewHelper helperParent) 
	{
		super(dataCharacter, options, listenerAction);
	}
	
	// SelectionData - build selection view as graphic
	
	@Override
	protected boolean buildView(SelectionData dataSelection, JComponent componentCategory)
	{
		SelectionViewGraphic viewSelection = new SelectionViewGraphic();
		return buildContent(dataSelection, viewSelection, componentCategory, "");
	}	
}

class ViewHelperRebuild extends ViewHelperList
{
	// When popback adds a selection get two events for _ADD_SELECTION and also for _ADD_REGION
	// The _ADD_SELECTION is handled through the category view and the _ADD_REGION is unnecessary
	// So to suppress the _ADD_REGION {where the region is a category or selection
	// this flavour of ViewHelper does nothing
	
	ViewHelperRebuild(CharacterData dataCharacter, Options options, ActionListener listenerAction) 
	{
		super(dataCharacter, options, listenerAction);
	}
	
	@Override
	public boolean buildPanel(RegionData dataRegion, JComponent componentView)
	{
		// Remove everything from the view at this point
		componentView.removeAll();
		
		// Rebuild the region
		ViewHelper helperView = getViewHelper(ViewHelperTypes._LIST);
		return helperView.buildPanel(dataRegion, componentView);
	}
	
	// This is the entry point for a CategoryData building the view
	public boolean buildPanel(CategoryData dataCategory, JComponent componentContainer)
	{
		System.out.println(String.format("No need to rebuild %s", dataCategory.getName()));
		return true;
	}
	
	public boolean buildPanel(SelectionData dataSelection, CategoryView viewCategory, JComponent componentCategory)
	{
		System.out.println(String.format("No need to rebuild %s", dataSelection.getName()));
		return true;
	}
	
	public boolean buildPanel(SelectionDataProxy dataSelectionProxy, CategoryView viewCategory, JComponent componentContainer)
	{
		System.out.println(String.format("No need to rebuild %s", dataSelectionProxy.getName()));
		return true;
	}
}
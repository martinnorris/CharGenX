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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EventListener;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import generator.CharacterEvent.EventEditEnum;
import generator.CharacterEvent.EventSelectEnum;
import generator.Options.OptionsFontEnum;
import lpc.LayoutPercent;

public interface CategoryView
{
	public JComponent buildPanel(ActionListener listenerAction);
}

class CategoryViewPanel extends JPanel implements CategoryView, CharacterListener
{
	private static final long serialVersionUID = 8503326570422542770L;

	CategoryViewPanel(ViewHelper helperView, CategoryData dataCategory, Options optionsView)
	{
		m_helperView = helperView;
		m_dataCategory = dataCategory;
		m_optionsView = optionsView;
	}
	
	private ViewHelper m_helperView = null;
	protected CategoryData m_dataCategory = null;
	protected Options m_optionsView = null;
	
	protected JComponent m_componentContent = null;
	
	@Override
	public JComponent buildPanel(ActionListener listenerAction)
	{
		JPanel panelDecorated = decoratePanel(this);
		
		// The content might be put into a sub panel of this
		m_componentContent = createPanel(panelDecorated);
		
		// Get the view helper to populate the category
		if (!m_helperView.buildContent(m_dataCategory, this, m_componentContent)) throw new RuntimeException(String.format("Could not build content for %s", m_dataCategory));
		
		// Listen for changes in the data model for doing edits to the structure
		m_dataCategory.addNodeListener(this);
		
		// Create listeners for the category
		if (!buildEnd(m_dataCategory, m_componentContent, listenerAction)) throw new RuntimeException(String.format("Could not add listener to %s", m_dataCategory));
		
		return this;
	}
	
	protected JPanel decoratePanel(JPanel panelCategory)
	{
		// Create decorations for panel for category
		Border borderCategory = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1), BorderFactory.createEmptyBorder(2, 2, 2, 2));
		panelCategory.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		panelCategory.setBorder(borderCategory);
		
		JPanel panelLabel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelLabel.add(getLabel(m_optionsView, m_dataCategory.getName()));		
		panelCategory.add(panelLabel);
						
		return panelCategory;
	}
	
	protected JComponent createPanel(JPanel panelCategory)
	{
		// The default component to populate is this panel
		return panelCategory;
	}
	
	protected boolean buildEnd(CategoryData dataCategory, JComponent componentCategory, ActionListener listenerAction)
	{
		return true;
	}
	
	protected JLabel getLabel(Options optionsView, String scLabel)
	{
		JLabel labelCategory = new JLabel();
		Font fontLabel = optionsView.getFont(labelCategory, OptionsFontEnum._TEXT);
		labelCategory.setFont(fontLabel);
		scLabel = String.format("%c%s", Character.toUpperCase(scLabel.charAt(0)), scLabel.substring(1));
		labelCategory.setText(scLabel);
		return labelCategory;
	}
	
	@Override
	public boolean changeView(CharacterEventView eventView)
	{
		return true;
	}
	
	@Override
	public boolean madeEdit(CharacterEventEdit eventCharacter) 
	{
		EventEditEnum enumEdit = eventCharacter.getType();
		
		switch (enumEdit)
		{
		case _ADD_SELECTION:
		case _REMOVE_SELECTION:
			// Check change is for this category
			SelectionData dataSelection = (SelectionData)eventCharacter.getSource();
			CategoryData dataCategory = dataSelection.getParent();
			if (!m_dataCategory.sameCategory(dataCategory)) return true;
			return rebuildContent();
		case _REMOVE_REGION:
			RegionData dataRegion = eventCharacter.getSource();
			if (m_dataCategory!=dataRegion) return true;
			return dataRegion.removeNodeListener(this);
		case _ADD_REGION:
		default:
			break;
		}
		return true;
	}
	
	protected ActionListener getListener()
	{
		// Get listeners of ActionListener class where derived views need to return adapters
		ActionListener listenerAction = null;
		ActionListener[] aListeners = m_componentContent.getListeners(ActionListener.class);
		if (1==aListeners.length) listenerAction = aListeners[0];
		return listenerAction;
	}
	
	protected boolean rebuildContent()
	{
		// Get current content listeners - should be one because all actions go via Controls
		ActionListener listenerAction = getListener();
		
		// Tear down all the content
		removeAll();
		
		// Adds the content back into the container for the category
		m_componentContent = createPanel(this);
		
		// Need ViewHelper to provide call backs for correct population
		m_helperView.buildContent(m_dataCategory, this, m_componentContent);
		
		// Do not need to add listener to category again
		
		// Add listeners for the selection events
		if (!buildEnd(m_dataCategory, m_componentContent, listenerAction)) throw new RuntimeException(String.format("Could not add listener to %s", m_dataCategory));
		
		revalidate();
		repaint();
		
		return true;
	}

	@Override
	public boolean madeSelection(CharacterEventSelect eventCharacter) 
	{
		SelectionData dataSelection = (SelectionData)eventCharacter.getSource();
		CategoryData dataCategory = dataSelection.getParent();
		if (!m_dataCategory.sameCategory(dataCategory)) return true;
		
		EventSelectEnum enumSelect = eventCharacter.getType();
		
		switch (enumSelect)
		{
		case _CHANGE_SELECTED:
		case _SET_EXCLUSION:
		case _SET_SUGGESTION:
			return changeValue(dataCategory, dataSelection);
		case _CHANGE_VALUE:
			return changeValue(dataCategory, dataSelection);
		}
		
		return true;
	}

	protected boolean changeSelection(CategoryData dataCategory, SelectionData dataSelection)
	{
		repaint();
		return true;
	}
	
	protected boolean changeValue(CategoryData dataCategory, SelectionData dataSelection)
	{
		Component[] aContent = m_componentContent.getComponents();
		
		for (Component component : aContent)
		{
			if (!(component instanceof SelectionView)) continue;
			SelectionView viewSelection = (SelectionView) component;
			SelectionData dataView = viewSelection.dataSelection();
			if (!dataSelection.sameSelection(dataView)) continue;
			
			// TODO change hints
			viewSelection.updatePanel(dataSelection, m_optionsView, "");

			// TODO content (value) change			
		}
		repaint();
		
		return true;
	}
	
	@Override
	public boolean failMessage(CharacterEventError eventError) 
	{
		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Panel with ");
		sb.append(m_dataCategory.toString());
		return sb.toString();
	}	
}

class CategoryViewBackground extends CategoryViewPanel
{
	private static final long serialVersionUID = -7762097715839364089L;

	CategoryViewBackground(ViewHelper helperView, CategoryData dataCategory, Options optionsView) 
	{
		super(helperView, dataCategory, optionsView);
	}

	@Override
	public void add(Component componentContent, Object constraints) 
	{
		// Just want to ignore the hints
		super.add(componentContent);
	}

	@Override
	protected JComponent createPanel(JPanel panelCategory)
	{
		// For the background the content is in a panel in a scroll pane in this panel
		
		JPanel panelContent = setBackgroundPanel(panelCategory);
		panelContent.setLayout(new LayoutPercent());

		JScrollPane paneScroll = new JScrollPane(panelContent);
		
		// Difficult to know what size to set for the scroll pane view
		Dimension dimSuggested = m_optionsView.getInitialSize();		
		Dimension dimBackground = panelContent.getPreferredSize();
		
		int iSizeX = (int)Math.min(dimSuggested.getWidth(), dimBackground.getWidth());
		int iSizeY = (int)Math.min(dimSuggested.getHeight(), dimBackground.getHeight());
		Dimension dimPreferred = new Dimension(iSizeX, iSizeY);
		
		panelContent.setPreferredSize(dimPreferred);
		paneScroll.setPreferredSize(dimPreferred);
		
		panelCategory.add(paneScroll);
		
		return panelContent;		
	}
	
	@Override
	protected JPanel decoratePanel(JPanel panelCategory)
	{
		// The background panel does not have any decoration
		return panelCategory;
	}
	
	/*
	@Override
	protected boolean buildEnd(CategoryData dataCategory, JComponent componentCategory, ActionListener listenerAction)
	{
		revalidate();
		return true;
	}
	*/
	
	private JPanel setBackgroundPanel(JPanel componentContainer) 
	{
		Path pathLoaded = m_optionsView.getResources();
		
		RegionData dataRegion = (RegionData)m_dataCategory.getParent();		
		String scName = dataRegion.getName();
		int iSuffix = scName.indexOf("_background");
		String scGraphic = scName.substring(0, iSuffix);
		
		return getBackgroundFile(pathLoaded, componentContainer, scGraphic);
	}
	
	private JPanel getBackgroundFile(Path pathLoaded, JPanel componentContainer, String scGraphic)
	{
		File folder = pathLoaded.toFile();
		File[] listOfFiles = folder.listFiles();
		
		for (File fileFound : listOfFiles)
		{
			if (!fileFound.isFile()) continue;
			String scFile = fileFound.getName();
			if (!scFile.startsWith(scGraphic)) continue;
			
			return getBackgroundImage(componentContainer, fileFound);
		}
		
		return componentContainer;
	}
	
	private JPanel getBackgroundImage(JPanel componentContainer, File fileGraphic)
	{
		BufferedImage imageLoaded;
		
		try 
		{
			imageLoaded = ImageIO.read(fileGraphic);
			return new CategoryViewBackgroundPanel(imageLoaded);
		} 
		catch (IOException e) 
		{
		}
		
		return componentContainer;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Background with ");
		sb.append(m_dataCategory.toString());
		return sb.toString();
	}
}

class CategoryViewBackgroundPanel extends JPanel
{
	private static final long serialVersionUID = 5117228844111551343L;

	CategoryViewBackgroundPanel(BufferedImage imageLoaded)
	{
		m_imageLoaded = imageLoaded;
	}
	
	private BufferedImage m_imageLoaded;
	private double m_dfScale = 1.0;
	
	public double setScale(double dfScale)
	{
		m_dfScale = Math.max(dfScale, 0.5); // Minimum 50%
		m_dfScale = Math.min(dfScale, 2.0); // Maximum 200%		
		return m_dfScale;
	}
	
	@Override
	public void paintComponent(Graphics g) 
	{
		super.paintComponent(g);
		
		int iScaleX = (int)(m_dfScale * m_imageLoaded.getWidth());
		int iScaleY = (int)(m_dfScale * m_imageLoaded.getHeight());
		
		g.drawImage(m_imageLoaded, 0, 0, iScaleX, iScaleY, null);
		
		return;
	}
	
	@Override
	public Dimension getPreferredSize() 
	{
		int iScaleX = (int)(m_dfScale * m_imageLoaded.getWidth());
		int iScaleY = (int)(m_dfScale * m_imageLoaded.getHeight());
		return new Dimension(iScaleX, iScaleY);
	}

/*					
	@Override
	public Dimension getMinimumSize() 
	{
		// Minimum size is 25%
		int iScaleX = (int)(0.25 * m_dfScale * m_imageLoaded.getWidth());
		int iScaleY = (int)(0.25 * m_dfScale * m_imageLoaded.getHeight());
		return new Dimension(iScaleX, iScaleY);
	}
	
	@Override
	public Dimension getMaximumSize() 
	{
		// Minimum size is 200%
		int iScaleX = (int)(2.0 * m_dfScale * m_imageLoaded.getWidth());
		int iScaleY = (int)(2.0 * m_dfScale * m_imageLoaded.getHeight());
		return new Dimension(iScaleX, iScaleY);
	}
*/
}

class CategoryViewListComponent extends JList<SelectionViewBase> implements ListCellRenderer<SelectionViewBase>
{
	private static final long serialVersionUID = 5802418881930400861L;
	
	public static final String HEIGHT_FOR_LIST = BorderLayout.NORTH;
	public static final String HEIGHT_FOR_GRAPHIC = BorderLayout.CENTER;

	public CategoryViewListComponent(DefaultListModel<SelectionViewBase> listSelection, CategoryData dataCategory, Options optionsView)
	{
		super(listSelection);
		m_dataCategory = dataCategory;
		m_optionsView = optionsView;
	}
	
	private CategoryData m_dataCategory = null;
	private Options m_optionsView = null;

	private String m_scHeightHint = HEIGHT_FOR_LIST;
	
	public String getSelectedCommand(SelectionView viewSelection)
	{
		SelectionData dataSelection = viewSelection.dataSelection();
		boolean zSelected = dataSelection.isSelected();
		String scCommand = String.format("Select '%s' '%s' %s", m_dataCategory.getName(), dataSelection.getName(), zSelected?"false":"true");
		return scCommand;
	}
	
	@Override
	public Component getListCellRendererComponent(JList<? extends SelectionViewBase> list, SelectionViewBase labelValue, int iIndex, boolean isSelected, boolean cellHasFocus) 
	{
		if (null==labelValue) return new JLabel("No items added");

		// Index -1 is the selected value
		if (0>iIndex) return labelValue;
		if (m_dataCategory.getChildCount()<=iIndex) return labelValue;
		
		// Get index value in category list
		SelectionData dataSelection = labelValue.dataSelection();
		
		// Cannot use Default renderer when want to change colour in selection box
		Color colourChosen = SelectionViewBase.chooseColour(m_optionsView, dataSelection, Color.WHITE);
		labelValue.setBackground(colourChosen);
		
		return labelValue;
	}

	public String setHeightHint(String scHeightHint) 
	{
		m_scHeightHint = scHeightHint;
		return m_scHeightHint;
	}
	
	public void layoutCategoryViewListComponent(JComponent componentContainer, JComponent componentContent)
	{
		// Only list has layout hint dependent on list content
		componentContainer.add(componentContent, m_scHeightHint);
	}
}

class CategoryViewListListener extends MouseAdapter
{
	public CategoryViewListListener(ActionListener listenerAction)
	{
		m_listenerAction = listenerAction;
	}
	
	private ActionListener m_listenerAction = null;
	
	public ActionListener getListener()
	{
		return m_listenerAction;
	}
	
	@Override
	public void mouseClicked(MouseEvent eventMouse)
	{
		Object objectSource = eventMouse.getSource();
		
		CategoryViewListComponent listCategory = (CategoryViewListComponent)objectSource;
		SelectionView viewSelection = listCategory.getSelectedValue();
		String scCommand = listCategory.getSelectedCommand(viewSelection);

		ActionEvent eventAction = new ActionEvent(objectSource, ActionEvent.ACTION_PERFORMED, scCommand);
		
		m_listenerAction.actionPerformed(eventAction);
		
		JComponent component = (JComponent)objectSource;
		component.repaint();
		
		return;
	}
}

class CategoryViewList extends CategoryViewPanel
{
	private static final long serialVersionUID = -7167890110242977801L;
	
	CategoryViewList(ViewHelper helperView, CategoryData dataCategory, Options optionsView)
	{
		super(helperView, dataCategory, optionsView);
	}
	
	@Override
	protected JPanel decoratePanel(JPanel panelCategory)
	{
		// Create decorations for panel for category
		return panelCategory;
	}
	
	@Override
	protected JComponent createPanel(JPanel panelCategory)
	{
		DefaultListModel<SelectionViewBase> listSelection = new DefaultListModel<SelectionViewBase>();
		CategoryViewListComponent componentSelections = new CategoryViewListComponent(listSelection, m_dataCategory, m_optionsView);
		componentSelections.setCellRenderer(componentSelections);
		componentSelections.setFocusable(false);
		componentSelections.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// JScrollPane scrollPane = new JScrollPane(componentSelections);
		
		// 1. Add to BoxLayout
		//panelCategory.setLayout(new BoxLayout(panelCategory, BoxLayout.PAGE_AXIS));
		//panelCategory.add(panelLabel);
		//panelCategory.add(scrollPane);
		// ... but extra space is given between label and content
		
		// 2. Panel holding label and list scroll pane and glue to stretch
		//JPanel panelContent = new JPanel();
		//panelContent.setLayout(new BoxLayout(panelContent, BoxLayout.PAGE_AXIS));
		//panelContent.add(panelLabel);
		//panelContent.add(scrollPane);
		//panelContent.add(Box.createVerticalGlue());
		//panelCategory.setLayout(new BoxLayout(panelCategory, BoxLayout.PAGE_AXIS));
		//panelCategory.add(panelContent);
		// ... but extra space still used between components
		
		// 3A. As BorderLayout center
		//panelCategory.setLayout(new BorderLayout());
		//panelCategory.add(panelLabel, BorderLayout.NORTH);
		//panelCategory.add(scrollPane, BorderLayout.CENTER);
		// ... not bad, but list size is maximized to available size
		
		// 3B. As BorderLayout center
		//panelCategory.setLayout(new BorderLayout());
		//panelCategory.add(panelLabel, BorderLayout.NORTH);
		//panelCategory.add(scrollPane, BorderLayout.SOUTH);
		// ... stretchy space between label and content
		
		// 4. As a GridBagLayout where extra space is absorbed by panel filler
//		panelCategory.setLayout(new GridBagLayout());
//		GridBagConstraints constraintL = new GridBagConstraints();
//		constraintL.fill = GridBagConstraints.HORIZONTAL;
//		constraintL.gridx = 0;
//		constraintL.gridy = 0;
//		constraintL.gridwidth = 1;
//		constraintL.weightx = 1.0;
//		constraintL.weighty = 0.0;
//		panelCategory.add(panelLabel, constraintL);
//		GridBagConstraints constraintP = new GridBagConstraints();
//		constraintP.fill = GridBagConstraints.HORIZONTAL;
//		constraintP.gridx = 0;
//		constraintP.gridy = 1;
//		constraintP.gridwidth = 1;
//		constraintP.weightx = 1.0;
//		constraintP.weighty = 0.0;
//		panelCategory.add(scrollPane, constraintP);
//		GridBagConstraints constraintF = new GridBagConstraints();
//		constraintF.fill = GridBagConstraints.HORIZONTAL;
//		constraintF.gridx = 0;
//		constraintF.gridy = 2;
//		constraintF.gridwidth = 1;
//		constraintF.weightx = 1.0;
//		constraintF.weighty = 1.0;
//		panelCategory.add(new JPanel(), constraintF);
		// ... works well except when have graphic content which is constrained
		
		// 5. As a GridBagLayout WITHOUT panel filler
//		panelCategory.setLayout(new GridBagLayout());
//		GridBagConstraints constraintL = new GridBagConstraints();
//		constraintL.fill = GridBagConstraints.HORIZONTAL;
//		constraintL.gridx = 0;
//		constraintL.gridy = 0;
//		constraintL.gridwidth = 1;
//		constraintL.weightx = 1.0;
//		constraintL.weighty = 0.0;
//		panelCategory.add(panelLabel, constraintL);
//		GridBagConstraints constraintP = new GridBagConstraints();
//		constraintP.fill = GridBagConstraints.HORIZONTAL;
//		constraintP.gridx = 0;
//		constraintP.gridy = 1;
//		constraintP.gridwidth = 1;
//		constraintP.weightx = 1.0;
//		constraintP.weighty = 0.0;
//		panelCategory.add(scrollPane, constraintP);
		// ... but appears in the middle surrounded by space
		
		// 6. Simple grid layout x rows 1 column
		//panelCategory.setLayout(new GridLayout(0,1));
		//panelCategory.add(panelLabel);
		//panelCategory.add(scrollPane);
		// ... but has stretchy space between label and content
		
		// 7. Spring layout
		//SpringLayout layoutSpring = new SpringLayout();
		//panelCategory.setLayout(layoutSpring);
		//panelCategory.add(panelLabel);
		//panelCategory.add(scrollPane);
		//SpringUtilities.makeCompactGrid(panelCategory, 2, 1, 5, 5, 5, 5);
		// ... but has stretchy space between label and content
		
		// 8. As BorderLayout center but with separate panel for content
		//panelCategory.setLayout(new BorderLayout());
		//panelCategory.add(panelLabel, BorderLayout.NORTH);
		//JPanel panelContent = new JPanel();
		//panelContent.add(scrollPane);
		//panelCategory.add(panelContent, BorderLayout.CENTER);
		// ... width of list not maximised
		
		// 9. As BorderLayout center but with separate panel for content also border layout at the top
		//panelCategory.setLayout(new BorderLayout());
		//panelCategory.add(panelLabel, BorderLayout.NORTH);
		//JPanel panelContent = new JPanel();
		//panelContent.setLayout(new BorderLayout());
		//panelContent.add(scrollPane, BorderLayout.NORTH);
		//panelCategory.add(panelContent, BorderLayout.CENTER);
		// ... works well except when have graphic content which is constrained
		
		// Build 9 is used in a callback when populated so know the content type
		
		return componentSelections;
	}
	
	protected boolean changeValue(CategoryData dataCategory, SelectionData dataSelection)
	{
		CategoryViewListComponent componentSelections = (CategoryViewListComponent)m_componentContent;
		ListModel<SelectionViewBase> listSelections = componentSelections.getModel();
		
		int iTotal = listSelections.getSize();
		
		for (int iIndex = 0; iIndex<iTotal; ++iIndex)
		{
			SelectionViewBase viewSelection = listSelections.getElementAt(iIndex);
			SelectionData dataView = viewSelection.dataSelection();
			if (!dataSelection.sameSelection(dataView)) continue;
			
			// TODO change hints
			viewSelection.updatePanel(dataSelection, m_optionsView, "");
			
			// TODO content change
		}
		
		repaint();
		
		return true;
	}
	
	@Override
	protected ActionListener getListener()
	{
		// CategoryViewListComponent uses a mouse adapter so get those and then find their listeners
		MouseListener[] aListeners = m_componentContent.getListeners(MouseListener.class);

		for (int iIndex= 0; iIndex<aListeners.length; ++iIndex)
		{
			EventListener listener = aListeners[iIndex];
			if (listener instanceof CategoryViewListListener)
			{
				CategoryViewListListener listenerAdapter = (CategoryViewListListener)listener;
				return listenerAdapter.getListener();
			}
		}
		
		return null;
	}
	
	@Override
	protected boolean buildEnd(CategoryData dataCategory, JComponent componentCategory, ActionListener listenerAction)
	{
		CategoryViewListComponent componentSelections = (CategoryViewListComponent)componentCategory;

		JPanel panelLabel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelLabel.add(getLabel(m_optionsView, m_dataCategory.getName()));
		
		DefaultListModel<SelectionViewBase> listSelection = (DefaultListModel<SelectionViewBase>)componentSelections.getModel();
		int iSelectionCount = listSelection.getSize();		
		if (10<iSelectionCount) iSelectionCount = 10;
		componentSelections.setVisibleRowCount(iSelectionCount);
		componentSelections.addMouseListener(new CategoryViewListListener(listenerAction));

		JScrollPane scrollPane = new JScrollPane(m_componentContent);

		Border borderCategory = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setBorder(borderCategory);
		
		setLayout(new BorderLayout());
		add(panelLabel, BorderLayout.NORTH);
		JPanel panelContent = new JPanel();
		panelContent.setLayout(new BorderLayout());
		// Do this because the place in the panelContent is NORTH for a normal list but CENTER for a graphical list
		componentSelections.layoutCategoryViewListComponent(panelContent, scrollPane);
		add(panelContent);

		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("List for ");
		sb.append(m_dataCategory.toString());
		return sb.toString();
	}
}

class CategoryViewComboComponent extends JComboBox<SelectionViewBase> implements ListCellRenderer<SelectionViewBase>
{
	private static final long serialVersionUID = 6259930201634156420L;
	
	CategoryViewComboComponent(CategoryData dataCategory, Options optionsView)
	{
		m_dataCategory = dataCategory;
		m_optionsView = optionsView;
	}
	
	private CategoryData m_dataCategory = null;
	private Options m_optionsView = null;

	public String getSelectedCommand(SelectionView viewSelection)
	{
		SelectionData dataSelection = viewSelection.dataSelection();
		boolean zSelected = dataSelection.isSelected();
		String scCommand = String.format("Select '%s' '%s' %s", m_dataCategory.getName(), dataSelection.getName(), zSelected?"false":"true");
		return scCommand;
	}
	
	@Override
	public Component getListCellRendererComponent(JList<? extends SelectionViewBase> listLabels, SelectionViewBase labelValue, int iIndex, boolean isSelected, boolean cellHasFocus) 
	{
		if (null==labelValue) return new JLabel("No items added");

		// Index -1 is the selected value
		if (0>iIndex) return labelValue;
		if (m_dataCategory.getChildCount()<=iIndex) return labelValue;
		
		// Get index value in category list
		SelectionData dataSelection = labelValue.dataSelection();
		
		// Cannot use Default renderer when want to change colour in selection box
		Color colourChosen = SelectionViewBase.chooseColour(m_optionsView, dataSelection, Color.WHITE);
		labelValue.setBackground(colourChosen);
		
		return labelValue;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("List for ");
		sb.append(m_dataCategory.toString());
		return sb.toString();
	}
}

class CategoryViewComboListener implements ActionListener
{
	public CategoryViewComboListener(ActionListener listenerAction)
	{
		m_listenerAction = listenerAction;
	}
	
	private ActionListener m_listenerAction = null;

	@Override
	public void actionPerformed(ActionEvent eventAction) 
	{
		Object objectSource = eventAction.getSource();
		
		CategoryViewComboComponent comboCategory = (CategoryViewComboComponent)objectSource;
		int iIndex = comboCategory.getSelectedIndex();
		SelectionView viewSelection = comboCategory.getItemAt(iIndex);

		String scCommand = comboCategory.getSelectedCommand(viewSelection);
		eventAction = new ActionEvent(viewSelection, ActionEvent.ACTION_PERFORMED, scCommand);
		
		m_listenerAction.actionPerformed(eventAction);
		
		return;
	}
}

class CategoryViewCombo extends CategoryViewPanel
{
	private static final long serialVersionUID = 250281658654490538L;

	CategoryViewCombo(ViewHelper helperView, CategoryData dataCategory, Options optionsView)
	{
		super(helperView, dataCategory, optionsView);
	}
	
	protected JPanel decoratePanel(JPanel panelCategory)
	{
		panelCategory.setLayout(new BorderLayout());
		
		// Create decorations for panel for combo
		Border borderCategory = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panelCategory.setBorder(borderCategory);
		
		JPanel panelLabel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelLabel.add(getLabel(m_optionsView, m_dataCategory.getName()));		
		panelCategory.add(panelLabel, BorderLayout.NORTH);
						
		return panelCategory;
	}
	
	@Override
	protected JComponent createPanel(JPanel panelCategory)
	{
		CategoryViewComboComponent componentSelections = new CategoryViewComboComponent(m_dataCategory, m_optionsView);
		componentSelections.setRenderer(componentSelections);		
		componentSelections.setEditable(false);
		componentSelections.setFocusable(false);
		
		// 1. As BorderLayout in North
		//panelCategory.setLayout(new BorderLayout());
		//panelCategory.add(componentSelections, BorderLayout.NORTH);
		// ... but also has big space at the bottom
		
		// 1.B As BorderLayout in Center - but set max height to preferred
		//Dimension dimensionPreferred = componentSelections.getPreferredSize();
		//Dimension dimensionMaximum = componentSelections.getMaximumSize();
		//dimensionMaximum.height = dimensionPreferred.height;
		//componentSelections.setMaximumSize(dimensionMaximum);
		//panelCategory.setLayout(new BorderLayout());
		//panelCategory.add(componentSelections, BorderLayout.CENTER);
		// ... but still takes up all the space
		
		// 1.C As BorderLayout in North - with a filler in the south
		//panelCategory.setLayout(new BorderLayout());
		//panelCategory.add(componentSelections, BorderLayout.NORTH);
		//JPanel panelFillingC = new JPanel();
		//panelFillingC.setMaximumSize(new Dimension(100,5));
		//panelCategory.add(panelFillingC, BorderLayout.CENTER);
		//JPanel panelFillingS = new JPanel();
		//panelFillingS.setMaximumSize(new Dimension(100,5));
		//panelCategory.add(panelFillingS, BorderLayout.SOUTH);
		// ... but still has big center space
		
		// 2. As a panel in a panel the top panel is box layout
		//JPanel panelLayout = new JPanel();
		//panelLayout.setLayout(new BorderLayout());
		//panelLayout.add(componentSelections, BorderLayout.NORTH);	
		//panelCategory.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		//panelCategory.add(panelLayout);
		//panelCategory.add(Box.createVerticalGlue());
		// ... no difference from 1 also has big space at the bottom
		
		// 3. GridLayout
		//panelCategory.setLayout(new GridLayout(1,1));
		//panelCategory.add(componentSelections);
		// ... but size is stretchy and starts off too high
		
		// 4. GridBagLayout
		//panelCategory.setLayout(new GridBagLayout());
		//GridBagConstraints constraint = new GridBagConstraints();
		//constraint.fill = GridBagConstraints.HORIZONTAL;
		//constraint.gridx = 0;
		//constraint.gridy = 0;
		//constraint.gridwidth = 1;
		//constraint.weightx = 1.0;
		//constraint.weighty = 0.0;
		//panelCategory.add(componentSelections, constraint);
		// Surrounds the component with lots of hard space
		
		// 5. Set _this_ size
		Dimension dimensionPreferred = componentSelections.getPreferredSize();
		Dimension dimensionMaximum = componentSelections.getMaximumSize();
		dimensionMaximum.height = dimensionPreferred.height;
		panelCategory.setMaximumSize(dimensionMaximum);
		panelCategory.add(componentSelections, BorderLayout.CENTER);
		// WORKS!!!
		
		return componentSelections;
	}
	
	@Override
	protected boolean buildEnd(CategoryData dataCategory, JComponent componentCategory, ActionListener listenerAction)
	{
		CategoryViewComboComponent componentSelections = (CategoryViewComboComponent)componentCategory;
		componentSelections.addActionListener(new CategoryViewComboListener(listenerAction));
		return true;
	}

	protected boolean changeValue(CategoryData dataCategory, SelectionData dataSelection)
	{
		CategoryViewComboComponent componentSelections = (CategoryViewComboComponent)m_componentContent;
		ListModel<SelectionViewBase> listSelections = componentSelections.getModel();
		
		int iTotal = listSelections.getSize();
		
		for (int iIndex = 0; iIndex<iTotal; ++iIndex)
		{
			SelectionViewBase viewSelection = listSelections.getElementAt(iIndex);
			SelectionData dataView = viewSelection.dataSelection();
			if (!dataSelection.sameSelection(dataView)) continue;
			
			// TODO change hints
			viewSelection.updatePanel(dataSelection, m_optionsView, "");
			break;
			
			// TODO content change
		}
		
		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Combo with ");
		sb.append(m_dataCategory.toString());
		return sb.toString();
	}
}

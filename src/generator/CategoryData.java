package generator;

import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import generator.CharacterEvent.EventEditEnum;

class CategoryData extends RegionData
{
	private static final long serialVersionUID = -8302299526539747098L;
	public static final String sm_scIdentifier = "CATEGORY";

	public CategoryData(String scName)
	{
		// Constructor used by proxy which should never need an alternative root
		super(scName);		
	}
	
	public CategoryData(String scName, RegionData dataRegionRoot)
	{
		super(scName);
		m_dataRegionRoot = dataRegionRoot;
	}
	
	// When the category is created in the character but not connected in the tree have an alternative way of finding the root
	public RegionData m_dataRegionRoot = null;
	
	@Override
	protected boolean removeThis(RegionData dataParent)
	{
		// Keep the node parent for sending events
		setParent(dataParent);
		return super.removeThis(dataParent);
	}
	
	public boolean sameCategory(RegionData dataRegionFind) 
	{
		return this==dataRegionFind;
	}
	
	@Override
	public boolean addRegionWithView(NotifyData dataNotify, String scPath, RegionData dataRegion)
	{
		if (!super.addRegionWithView(dataNotify, scPath, dataRegion)) return false;
		// Just in case the selection already exists
		RegionData dataRegionSelection = getRegion(dataRegion.getName());
		return dataNotify.fireCharacterEvent(new CharacterEventEdit(EventEditEnum._ADD_SELECTION, dataRegionSelection));
	}
	
	@Override
	public boolean removeRegion(NotifyData dataNotify, RegionData dataRegion)
	{
		remove(dataRegion);
		if (!dataRegion.removeThis(this)) return false;
		return dataNotify.fireCharacterEvent(new CharacterEventEdit(EventEditEnum._REMOVE_SELECTION, dataRegion));
	}
		
	@Override
	public NotifyData getNotify()
	{
		// When the category does not appear it won't be attached into the tree
		NotifyData dataRegion = (NotifyData) getParent();
		if (null==dataRegion) return m_dataRegionRoot.getNotify();
		return dataRegion.getNotify();
	}

	@Override
	public WhorlEngine getWhorlEngine()
	{
		// There are some cases where the WhorlEngine cannot be found from the tree
		// e.g. if the selection is only represented by proxies then the real
		// selections will have a parent category, but that parent is not in the tree
		// So need to provide the real category a way to find the tree root
		RegionData dataRegion = (RegionData) getParent();
		if (null==dataRegion) return m_dataRegionRoot.getWhorlEngine();
		return dataRegion.getWhorlEngine();
	}
	

	public boolean setSuggestion(SelectionData dataSelected, SelectionDataEvent eventSelection) 
	{
		for (@SuppressWarnings("unchecked")
		Enumeration<RegionData> enumerateRegion = children(); enumerateRegion.hasMoreElements(); )
		{
			SelectionData dataSelection = (SelectionData)enumerateRegion.nextElement();
			dataSelection.setSuggestion(eventSelection, dataSelected);
		}
		return true;
	}
	
	public boolean setExclusion(SelectionData dataSelected, SelectionDataEvent eventSelection) 
	{
		for (@SuppressWarnings("unchecked")
		Enumeration<RegionData> enumerateRegion = children(); enumerateRegion.hasMoreElements(); )
		{
			SelectionData dataSelection = (SelectionData)enumerateRegion.nextElement();
			dataSelection.setExclusion(eventSelection, dataSelected);
		}
		return true;
	}
	
	public boolean setUnique(NotifyData notifyData, SelectionData dataUnique)
	{
		for (@SuppressWarnings("unchecked")
		Enumeration<RegionData> enumerateRegion = children(); enumerateRegion.hasMoreElements(); )
		{
			SelectionData dataSelection = (SelectionData)enumerateRegion.nextElement();
			if (dataUnique==dataSelection) continue;
			dataSelection.setSelected(notifyData, false);
		}
		return true;		
	}
	
	public boolean resetSelection(RegionData dataNotify) 
	{
		for (@SuppressWarnings("unchecked")
		Enumeration<RegionData> enumerateRegion = children(); enumerateRegion.hasMoreElements(); )
		{
			SelectionData dataSelection = (SelectionData)enumerateRegion.nextElement();
			dataSelection.setSelected(dataNotify, false);
		}
		
		return true;
	}
	
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleCategory(getName(), this, sm_scIdentifier);
	}
	
	@Override
	public boolean persistContent(PersistHelper helperPersist) 
	{		
		Iterator<RegionData> iterateSelection = getRegions();
		
		while (iterateSelection.hasNext())
		{
			SelectionData dataSelection = (SelectionData)iterateSelection.next();
			dataSelection.persistData(helperPersist);	
		}
		
		return true;
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		// There are no tags for category
		return false;
	}
	
	@Override
	public boolean handleView(ViewHelper helperView, JComponent componentView) 
	{
		return helperView.buildPanel(this, componentView);
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		// Explicitly set the merge root
		m_dataRegionRoot = helperMerge.requireExistingRoot();
		return super.handleMerge(dataRegionParent, helperMerge, dataCharacterMerge, dataRegionMerge);
	}
}

class CategoryDataProxy extends CategoryData
{
	private static final long serialVersionUID = -433358974878941130L;

	// For XML there is no difference between category and category proxy
	// The difference is made with the merge
	
	CategoryDataProxy(CategoryData dataCategory)
	{
		super(dataCategory.getName());
		m_dataCategory = dataCategory;
		m_dataStore = dataCategory;
	}
	
	private CategoryData m_dataCategory = null;
	private CategoryData m_dataStore = null;
	private boolean m_zContainedSelections = true;
	
	@Override
	public boolean sameCategory(RegionData dataCategory) 
	{
		return m_dataCategory.sameCategory(dataCategory);
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		if (m_zContainedSelections)	return helperPersist.handleCategory(getName(), m_dataCategory, sm_scIdentifier);
		return helperPersist.handleCategory(getName(), m_dataStore, sm_scIdentifier);
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		// Make sure have valid proxy - actual category updated previously
		m_dataCategory = helperMerge.requireExistingCategory(dataCharacterMerge, m_dataCategory.getName());
		return true;
	}
	
	@Override
	public void add(MutableTreeNode dataSelection)
	{
		if (dataSelection instanceof SelectionDataProxy)
		{
			// If selection is a proxy then from here need to populate separate selections
			if (m_zContainedSelections) m_dataStore = new CategoryData(String.format("%s_proxy", m_dataCategory.getName()));
			m_zContainedSelections = false;
			m_dataStore.add(dataSelection);
			// A proxy would have already been added to the underlying category
			return;
		}
		m_dataCategory.add(dataSelection);
		return;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Enumeration<RegionData> children() 
	{
		return m_dataStore.children();
	}

	@Override
	public boolean getAllowsChildren() 
	{
		return m_dataStore.getAllowsChildren();
	}

	@Override
	public TreeNode getChildAt(int childIndex) 
	{
		return m_dataStore.getChildAt(childIndex);
	}

	@Override
	public int getChildCount() 
	{
		return m_dataStore.getChildCount();
	}

	@Override
	public int getIndex(TreeNode node) 
	{
		return m_dataStore.getIndex(node);
	}

	@Override
	public void insert(MutableTreeNode child, int index) 
	{
		m_dataStore.insert(child, index);
		return;
	}

	@Override
	public void remove(int index) 
	{
		m_dataStore.remove(index);
		return;
	}

	@Override
	public void remove(MutableTreeNode node) 
	{
		// TODO why try and remove nodes that are not members?
		if (m_dataStore.isNodeChild(node)) m_dataStore.remove(node);
		if (m_dataCategory.isNodeChild(node)) m_dataCategory.remove(node);
		return;
	}
}


package generator;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;

import generator.CharacterEvent.EventEditEnum;
import generator.CharacterEvent.EventSelectEnum;

class CharacterData extends DefaultTreeModel implements Serializable, CharacterHelper
{
	private static final long serialVersionUID = -6193053100011390574L;
	public static final String sm_scIdentifier = "CHARACTER";

	CharacterData()
	{
		super(new RegionDataRoot("Form"));
		m_mapCategory = new HashMap<String, CategoryData>();
		m_helperEvaluation = EvaluationHelper.newEvaluationHelper(this);
		m_whorlEngine = WhorlEngine.getWhorlEngine(this);
		m_currency = new CharacterDataCurrency();
		
		RegionDataRoot dataRoot = (RegionDataRoot)getRoot();
		dataRoot.setWhorlEngine(m_whorlEngine);
	}
	
	private HashMap<String, CategoryData> m_mapCategory = null;
	private transient EvaluationHelper m_helperEvaluation = null;
	private transient WhorlEngine m_whorlEngine = null; 
	private CharacterDataCurrency m_currency = null;
	
	private void readObject(ObjectInputStream streamIn) throws IOException, ClassNotFoundException
	{
		streamIn.defaultReadObject();
		m_helperEvaluation = EvaluationHelper.newEvaluationHelper(this);
		m_whorlEngine = WhorlEngine.getWhorlEngine(this);
		RegionDataRoot dataRoot = (RegionDataRoot)getRoot();
		dataRoot.setWhorlEngine(m_whorlEngine);
		return;
	}
	
	public RegionData getRoot()
	{
		RegionData dataRegion = (RegionData) super.getRoot();
		return dataRegion;
	}
		
	public boolean addListener(CharacterListener listener)
	{
		RegionData dataRoot = getRoot();
		
		CharacterModelListenerAdapter adaptModelListener = new CharacterModelListenerAdapter(listener);
		this.addTreeModelListener(adaptModelListener);
		
		return dataRoot.addNodeListener(listener);
	}
		
	
	public boolean hasCategory(String scName)
	{
		scName = scName.intern();
		return m_mapCategory.containsKey(scName);		
	}
	
	public CategoryData putCategory(String scCategory, CategoryData dataCategory)
	{
		m_mapCategory.put(scCategory, dataCategory);
		dataCategory.fireCharacterEvent(new CharacterEventEdit(EventEditEnum._ADD_CATEGORY, dataCategory));
		return dataCategory;				
	}
	
	public CategoryData baseCategory(String scCategory)
	{
		RegionData dataRoot = getRoot();
		if (hasCategory(scCategory)) return getCategory(scCategory);		
		CategoryData dataCategory = new CategoryData(scCategory, dataRoot);
		return putCategory(scCategory, dataCategory);
	}
	
	public CategoryData referenceCategory(String scCategory)
	{
		CategoryData dataCategoryReturn = getCategory(scCategory);
		
		if (null==dataCategoryReturn)
		{
			dataCategoryReturn = baseCategory(scCategory);
		}
		
		dataCategoryReturn = new CategoryDataProxy(dataCategoryReturn);			
		
		return dataCategoryReturn;
	}
	
	public SelectionData referenceSelection(CategoryData dataCategory, String scSelection, String scView)
	{
		CategoryData dataBaseCategory = baseCategory(dataCategory.getName());
		SelectionData dataSelection = (SelectionData)dataBaseCategory.getRegion(scSelection);
		
		if (null==dataSelection)
		{
			dataSelection = new SelectionData(scSelection);
			dataBaseCategory.addRegion(dataBaseCategory, dataSelection);
		}
		
		SelectionDataProxy dataSelectionProxy = new SelectionDataProxy(dataSelection, scView);
		dataCategory.addRegion(dataSelectionProxy, dataSelectionProxy);		
		return dataSelectionProxy;
	}
	
	public Iterator<CategoryData> getCategories() 
	{
		return m_mapCategory.values().iterator();
	}
	
	public CategoryData getCategory(String scCategory)
	{
		scCategory = scCategory.intern();
		return m_mapCategory.get(scCategory);
	}
	
	private boolean removeCategory(CategoryData dataCategory)
	{
		m_mapCategory.remove(dataCategory.getName());
		dataCategory.fireCharacterEvent(new CharacterEventEdit(EventEditEnum._REMOVE_CATEGORY, dataCategory));
		return true;				
	}
	
	public boolean resetSelection() 
	{
		Iterator<CategoryData> iterateCategory = getCategories();
		RegionData dataNotify = getRoot();
		
		while (iterateCategory.hasNext())
		{
			CategoryData dataCategory = iterateCategory.next();
			if (!dataCategory.resetSelection(dataNotify)) return false;
		}

		return true;
	}
	
	public boolean removeAll()
	{
		RegionData dataRoot = getRoot();
		dataRoot.removeAll(dataRoot);
		
		Iterator<CategoryData> iterator = getCategories();
		
		while (iterator.hasNext())
		{
			CategoryData dataCategory = iterator.next();
			dataCategory.removeAll(dataRoot);
			iterator.remove();
			removeCategory(dataCategory);
		}
		
		return dataRoot.fireCharacterEvent(new CharacterEventEdit(EventEditEnum._REMOVE_ALL));
	}
	
	public boolean replaceData(CharacterData dataReplacement, File fileLoaded)
	{
		m_mapCategory = dataReplacement.m_mapCategory;
		
		RegionData dataOldRoot = getRoot();
		RegionData dataNewRoot = dataReplacement.getRoot();
		dataNewRoot.mergeNodeListener(dataOldRoot);		
		setRoot(dataNewRoot);
		
		return dataNewRoot.fireCharacterEvent(new CharacterEventLoaded(fileLoaded));
	}
	
	
	public EvaluationHelper getEvaluation()
	{
		return m_helperEvaluation;
	}

	public int getCurrencyValueBase(String scValue)
	{
		return m_currency.getValue(scValue);
	}

	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleData(this, sm_scIdentifier);
	}
	
	@Override
	public boolean persistContent(PersistHelper helperPersist) 
	{
		RegionData dataRoot = getRoot();
		return dataRoot.persistData(helperPersist);
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		return true;
	}
	
	@Override
	public boolean handleView(ViewHelper helperView, JComponent componentView) 
	{
		return helperView.buildPanel(this, componentView);
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		throw new RuntimeException("No merge helper");
	}
	
	public int exit() 
	{
		// TODO check for changes and save
		return 0;
	}
}

class CharacterModelPresets
{
	CharacterModelPresets()
	{
		m_ListPresets = new ArrayList<SelectionData>();
	}
	
	private ArrayList<SelectionData> m_ListPresets = null;
	
	public void addPreset(SelectionData dataRegion)
	{
		m_ListPresets.add(dataRegion);
	}
	
	public void presetValues(RegionData dataNotify)
	{
		for (SelectionData dataSelection : m_ListPresets)
			dataSelection.setSelected(dataNotify, true);
	}
}

class CharacterModelListenerAdapter implements TreeModelListener
{
	CharacterModelListenerAdapter(CharacterListener listenerData)
	{
		m_listenerData = listenerData;
	}
	
	private CharacterListener m_listenerData = null;
	
	@Override
	public void treeNodesChanged(TreeModelEvent eventModel) 
	{
		Object[] aobjectChanged = eventModel.getChildren();
		
		for (Object objectChanged : aobjectChanged)
		{
			RegionData dataRegion = (RegionData)objectChanged;
			m_listenerData.madeSelection(new CharacterEventSelect(EventSelectEnum._CHANGE_SELECTED, dataRegion));
		}
	}

	@Override
	public void treeNodesInserted(TreeModelEvent eventModel) 
	{
		Object[] aobjectChanged = eventModel.getChildren();
		
		for (Object objectChanged : aobjectChanged)
		{
			RegionData dataRegion = (RegionData)objectChanged;
			m_listenerData.madeEdit(new CharacterEventEdit(EventEditEnum._ADD_REGION, dataRegion));
		}
	}

	@Override
	public void treeNodesRemoved(TreeModelEvent eventModel) 
	{
		Object[] aobjectChanged = eventModel.getChildren();
		
		for (Object objectChanged : aobjectChanged)
		{
			RegionData dataRegion = (RegionData)objectChanged;
			m_listenerData.madeEdit(new CharacterEventEdit(EventEditEnum._REMOVE_REGION, dataRegion));
		}
	}

	@Override
	public void treeStructureChanged(TreeModelEvent eventModel) 
	{
		m_listenerData.madeEdit(new CharacterEventLoaded(null));	
	}	
}

class CharacterDataCurrency
{
	public CharacterDataCurrency() 
	{
		m_aiMultipliers = new int[] {1, 12, 20};
	}
	
	private int[] m_aiMultipliers = null;
	
	public int getValue(String scValue)
	{
		String[] ascValues = scValue.split("\\|");
		
		int iTotal = 0;
		
		for (int iIndex = ascValues.length; iIndex>0; --iIndex)
		{
			iTotal *= m_aiMultipliers[ascValues.length - iIndex];
			iTotal += Integer.parseInt(ascValues[iIndex-1]);
		}

		return iTotal;
	}
}

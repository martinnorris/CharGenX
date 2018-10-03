package generator;

import java.util.Iterator;

class MergeHelper
{
	MergeHelper(CharacterData dataCharacterExisting)
	{
		m_dataCharacterExisting = dataCharacterExisting;
	}
	
	private CharacterData m_dataCharacterExisting = null;
	
	/**
	 * Merge of data all done using call backs
	 * Merge the supplied parameter with the internal member m_dataChar 
	 */
	
	public boolean mergeData(CharacterData dataCharacterMerge)
	{
		// Provide root region as parent for all categories
		RegionData dataRootExisting = m_dataCharacterExisting.getRoot();
		
		// Do this first so that all categories and selections are available for merge of selection actions
		Iterator<CategoryData> iterateCategory = dataCharacterMerge.getCategories();
		
		while (iterateCategory.hasNext())
		{
			CategoryData dataCategory = iterateCategory.next();
			String scCategory = dataCategory.getName();
			
			if (!m_dataCharacterExisting.hasCategory(scCategory))
			{
				m_dataCharacterExisting.putCategory(scCategory, dataCategory);
			}
			
			CategoryData dataCategoryExisting = m_dataCharacterExisting.getCategory(scCategory);
			// Merge categories but only include selections 
			if (!dataCategoryExisting.handleMerge(dataRootExisting, this, dataCharacterMerge, dataCategory)) return false;
		}
		
		// Merge regions
		RegionData dataRootMerge = dataCharacterMerge.getRoot();
		if (!dataRootExisting.handleMerge(dataRootExisting, this, dataCharacterMerge, dataRootMerge)) return false;
				
		return true; // dataRootExisting.fireStructureEvent(new CharacterEventLoaded(EventEditEnum._UPDATE_ALL, null));
	}
	
	public boolean mergeData(CharacterData dataCharacterMerge, RegionData dataRegionExisting, RegionData dataRegionMerge)
	{
		Iterator<RegionData> iterateRegions = dataRegionMerge.getRegions();
		
		while (iterateRegions.hasNext())
		{
			RegionData dataSubRegionMerge = iterateRegions.next();
			RegionData dataSubRegionAdd = dataSubRegionMerge;
			String scSubRegion = dataSubRegionMerge.getName();
			
			if (!dataRegionExisting.hasRegion(scSubRegion))
			{
				RegionData dataNotify = m_dataCharacterExisting.getRoot();
				// Add region, no view
				dataRegionExisting.addRegion(dataNotify, dataSubRegionAdd);
			}
			
			RegionData dataSubRegionExisting = dataRegionExisting.getRegion(scSubRegion);
			dataSubRegionExisting.handleMerge(dataRegionExisting, this, dataCharacterMerge, dataSubRegionMerge);
		}
		
		return true;
	}
	

	public RegionData requireExistingRoot() 
	{
		return m_dataCharacterExisting.getRoot();
	}
	
	public RegionData requireExistingRegion(String scPath, String scRegion)
	{
		RegionData dataRoot = m_dataCharacterExisting.getRoot();
		String scRegionName = scPath + ":" + scRegion;
		RegionData dataRegion = dataRoot.getRegion(scRegionName);
		if (null==dataRegion)
		{
			dataRegion = new RegionData(scRegion);
			if (!dataRoot.addRegionWithView(dataRoot, scPath, dataRegion)) dataRoot.fireCharacterEvent(new CharacterEventError("Could not add " + scRegionName));
		}
		return dataRegion;
	}
	
	public CategoryData requireExistingCategory(CharacterData dataCharacterMerge, String scCategory)
	{
		CategoryData dataCategoryExisting = m_dataCharacterExisting.getCategory(scCategory);
		if (null==dataCategoryExisting)
		{
			CategoryData dataCategory = dataCharacterMerge.getCategory(scCategory);
			m_dataCharacterExisting.putCategory(scCategory, dataCategory);
			dataCategoryExisting = m_dataCharacterExisting.getCategory(scCategory);
			dataCategoryExisting.handleMerge(m_dataCharacterExisting.getRoot(), this, dataCharacterMerge, dataCategory);
		}
		return dataCategoryExisting;
	}
	
	public SelectionData requireExistingSelection(CharacterData dataCharacterMerge, String scCategory, String scSelection)
	{
		CategoryData dataCategoryExisting = requireExistingCategory(dataCharacterMerge, scCategory);
		SelectionData dataSelectionExisting = (SelectionData)dataCategoryExisting.getRegion(scSelection);
		if (null==dataSelectionExisting)
		{
			CategoryData dataCategory = dataCharacterMerge.getCategory(scCategory);
			SelectionData dataSelection = (SelectionData)dataCategory.getRegion(scSelection);
			RegionData dataNotify = m_dataCharacterExisting.getRoot();
			dataCategoryExisting.addRegionWithView(dataNotify, null, dataSelection);
			dataSelectionExisting = (SelectionData)dataCategoryExisting.getRegion(scSelection);
			dataSelectionExisting.handleMerge(dataCategoryExisting, this, dataCharacterMerge, dataSelection);
		}
		return dataSelectionExisting;
	}
}
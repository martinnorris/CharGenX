package generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import generator.CharacterEvent.EventEditEnum;

class RegionData extends NotifyData implements CharacterHelper, Comparable<RegionData>
{
	private static final long serialVersionUID = -1298928559852641492L;
	public static final String sm_scIdentifier = "REGION";

	public RegionData(String scName)
	{
		m_scName = scName;
	}
	
	private String m_scName = null;


	public String getName()
	{
		return m_scName;
	}

	
	public String getFirst(String scView, RegionData dataRegion)
	{
		if (null==scView) return dataRegion.getName();
		int iIndex = scView.indexOf(':');
		if (0>iIndex) return scView;
		return scView.substring(0, iIndex);
	}
	
	private String getRemainder(String scView)
	{
		int iIndex = scView.indexOf(':');
		if (0>iIndex) return null;
		return scView.substring(iIndex+1);
	}
	
	public String getLast(String scView)
	{
		int iIndex = scView.lastIndexOf(':');
		if (0>iIndex) return scView;
		return scView.substring(iIndex+1);
	}
	
	public String getPath(String scView)
	{
		int iIndex = scView.lastIndexOf(':');
		if (0>iIndex) return null;
		return scView.substring(0, iIndex);
	}
	
	private RegionData addRegionChild(NotifyData dataNotify, RegionData dataRegion)
	{
		String scRegion = dataRegion.getName();
		RegionData dataRegionChild = getRegion(scRegion);
		if (null!=dataRegionChild) return dataRegionChild;
		
		// If a category or selection is being moved then the parent was maintained for events so now need to reset parent before reattaching the node
		dataRegion.setParent(null);
		
		add(dataRegion);
		dataNotify.fireCharacterEvent(new CharacterEventEdit(EventEditEnum._ADD_REGION, dataRegion));
		return dataRegion;
	}
	
	private RegionData addRegionWithParent(NotifyData dataNotify, String scView, RegionData dataRegion)
	{
		if (null==scView) return addRegionChild(dataNotify, dataRegion);
		
		String scRegion = getFirst(scView, dataRegion);
		RegionData dataRegionParent = addRegionChild(dataNotify, new RegionData(scRegion));
		String scChild = getRemainder(scView);
		return dataRegionParent.addRegionWithParent(dataNotify, scChild, dataRegion);		
	}
		
	public boolean addRegion(NotifyData dataNotify, RegionData dataRegion)
	{
		if (null==addRegionChild(dataNotify, dataRegion)) return fireCharacterEvent(new CharacterEventError("Could not add " + dataRegion));
		return true;
	}
	
	public boolean addRegionWithView(NotifyData dataNotify, String scPath, RegionData dataRegion)
	{
		if (null==addRegionWithParent(dataNotify, scPath, dataRegion)) return fireCharacterEvent(new CharacterEventError("Could not add " + dataRegion));
		return true;
	}
	
	public boolean removeRegion(NotifyData dataNotify, RegionData dataRegion)
	{
		remove(dataRegion);
		if (!dataRegion.removeThis(this)) return false;
		return dataNotify.fireCharacterEvent(new CharacterEventEdit(EventEditEnum._REMOVE_REGION, dataRegion));
	}
	
	public Iterator<RegionData> getRegions()
	{
		@SuppressWarnings("unchecked")
		List<RegionData> listRegions = Collections.list(children());
		Collections.sort(listRegions);
		return listRegions.iterator();
	}
	
	public RegionData getRegion(String scRegion)
	{
		int iIndex = scRegion.indexOf(':');
		
		if (0<iIndex)
		{
			String scSubRegion = scRegion.substring(iIndex+1);
			
			String scParentRegion = scRegion.substring(0, iIndex);
			RegionData dataRegion = getRegion(scParentRegion);
			
			if (null==dataRegion) return null;
			return dataRegion.getRegion(scSubRegion);
		}
		
		//if (scRegion.equals(getName())) return this;
		
		Iterator<RegionData> iterateRegions = getRegions();
		
		while (iterateRegions.hasNext())
		{
			RegionData dataRegion = iterateRegions.next();
			if (scRegion.equals(dataRegion.getName())) return dataRegion;
		}

		return null;
	}
	
	public boolean hasRegion(String scRegion)
	{
		int iIndex = scRegion.indexOf(':');
		
		if (0<iIndex)
		{
			String scSubRegion = scRegion.substring(iIndex+1);
			
			String scParentRegion = scRegion.substring(0, iIndex);
			RegionData dataRegion = getRegion(scParentRegion);
			
			if (null==dataRegion) return false;
			return dataRegion.hasRegion(scSubRegion);
		}
		
		Iterator<RegionData> iterateRegions = getRegions();
		
		while (iterateRegions.hasNext())
		{
			RegionData dataRegion = iterateRegions.next();
			if (scRegion.equals(dataRegion.getName())) return true;
		}

		return false;
	}
	
	public boolean removeAll(NotifyData dataNotify)
	{
		Iterator<RegionData> iterateRegions = getRegions();
		
		while (iterateRegions.hasNext())
		{
			RegionData dataRegion = iterateRegions.next();
			dataRegion.removeAll(dataNotify);
			iterateRegions.remove();
			removeRegion(dataNotify, dataRegion);
		}
		
		return true;
	}
		
	public boolean detachRegions(NotifyData dataNotify)
	{
		Iterator<RegionData> iterateRegions = getRegions();
		
		while (iterateRegions.hasNext())
		{
			RegionData dataRegion = iterateRegions.next();
			iterateRegions.remove();
			removeRegion(dataNotify, dataRegion);
		}
		
		return true;
	}
	
	public WhorlEngine getWhorlEngine()
	{
		RegionData dataRegion = (RegionData)getParent();
		return dataRegion.getWhorlEngine();
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleRegion(m_scName, this, sm_scIdentifier);
	}
	
	@Override
	public boolean persistContent(PersistHelper helperPersist) 
	{
		Iterator<RegionData> iterateRegions = getRegions();

		while (iterateRegions.hasNext())
		{
			RegionData dataSubRegion = iterateRegions.next();
			dataSubRegion.persistData(helperPersist);
		}

		return true;
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		// There are no tags for regions
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
		return helperMerge.mergeData(dataCharacterMerge, this, dataRegionMerge);
	}

	
	@Override
	public int compareTo(RegionData dataRegion) 
	{
		return m_scName.compareTo(dataRegion.m_scName);
	}		
	
	
	@Override
	public String toString()
	{
		return m_scName;
	}
}

class RegionDataRoot extends RegionData
{
	private static final long serialVersionUID = 7604957202694769173L;

	public RegionDataRoot(String scName) 
	{
		super(scName);
		// The WhorlEngine helps with recalculation of SelectionData ValueDataMAth nodes
		m_listSortIndex = new ArrayList<RegionData>();
	}

	private List<RegionData> m_listSortIndex = null;

	// Whorl engine is set separately because requires character data
	
	private transient WhorlEngine m_whorlEngine = null; 
	
	public void setWhorlEngine(WhorlEngine whorlEngine) 
	{
		m_whorlEngine = whorlEngine;
	}
	
	public WhorlEngine getWhorlEngine()
	{
		return m_whorlEngine;
	}
	
	/**
	 * Override regions so they are sorted by creation order when they have a view {ie they are in a tab}
	 */
	
	@Override
	public boolean addRegionWithView(NotifyData dataNotify, String scPath, RegionData dataRegion)
	{
		if (!super.addRegionWithView(dataNotify, scPath, dataRegion)) return false;
		
		// A region view that starts with '_' is invisible - added as child, but not in visible list
		if ('_'==scPath.charAt(0)) return true;
			
		String scFirst = getFirst(scPath, dataRegion);
		
		// CANNOT use getRegion because any iteration in the class uses the list have not yet added the node
		@SuppressWarnings("unchecked")
		Enumeration<RegionData> enumerateRegions = children();
		
		while (enumerateRegions.hasMoreElements())
		{
			RegionData dataFirst = enumerateRegions.nextElement();
			if (!scFirst.equals(dataFirst.getName())) continue;
			
			if (m_listSortIndex.contains(dataFirst)) return true;
			m_listSortIndex.add(dataFirst);
			return true;
		}
		
		return false;	
	}
	
	@Override
	public boolean removeRegion(NotifyData dataNotify, RegionData dataRegion)
	{
		super.removeRegion(dataNotify, dataRegion);
		if (m_listSortIndex.contains(dataRegion)) m_listSortIndex.remove(dataRegion);
		return true;
	}
	
	public Iterator<RegionData> getUnsortedRegions()
	{
		return m_listSortIndex.listIterator();
	}
	
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		// First merge content of region as a normal region
		if (!helperMerge.mergeData(dataCharacterMerge, this, dataRegionMerge)) return fireCharacterEvent(new CharacterEventError("Root region could not merge members"));
		
		RegionDataRoot dataRootMerge = (RegionDataRoot)dataRegionMerge;
		
		// Gets the sort list
		Iterator<RegionData> iterateRegions = dataRootMerge.getUnsortedRegions();
		while (iterateRegions.hasNext())
		{
			RegionData dataSubRegionMerge = iterateRegions.next();
			String scSubRegion = dataSubRegionMerge.getName();
			
			RegionData dataSubRegionExisting = getRegion(scSubRegion);
			if (null==dataSubRegionExisting) return fireCharacterEvent(new CharacterEventError("Root region inconsistent members"));
			if (m_listSortIndex.contains(dataSubRegionExisting)) continue;
			
			m_listSortIndex.add(dataSubRegionExisting);
		}
		
		return true;
	}
}

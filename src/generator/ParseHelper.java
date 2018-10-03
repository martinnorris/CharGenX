package generator;

import java.util.ArrayList;
import java.util.List;

class ParseHelper
{
	public ParseHelper(CharacterData dataCharacter)
	{
		m_dataCharacter = dataCharacter;
		m_dataNotify = dataCharacter.getRoot();
		m_context = new ParseCharacter(null, 0, ParseModifiers._NONE);
		m_listContext = new ArrayList<ParseContext>();
		m_listContext.add(m_context);
	}

	private CharacterData m_dataCharacter = null;
	private RegionData m_dataNotify = null;
	private ParseContext m_context = null;
	private List<ParseContext> m_listContext = null;
	
	// Parse XML between different contexts
	
	public boolean startElement(String scType)
	{
		// Content available for new data and must be further data contained
		m_context = m_context.createContext(scType);
		
		return true;
	}
	
	public boolean contentElement(String scType, String scContent)
	{
		// Routine gets called with no content so just ignore this
		if (null==scType) return true;
		if (0==scContent.length()) return true;
		return m_context.populateContext(scType, scContent);
	}
	
	public RegionData setElement(String scType, String scAttribute, String scValue)
	{
		if (m_context.knownAttribute(scType, scAttribute, scValue)) return m_context.m_dataRegion;
		return null;
	}
	
	public boolean endElement(String scType)
	{
		m_context = m_context.previousContext(scType);
		return true;
	}
	
	// Parse text file between contexts
	
	public boolean addLine(String scLine)
	{
		// Snip off and count the spaces
		int iSpaceCount = countSpaces(scLine);
		String scContent = scLine.substring(iSpaceCount);

		if (0==scContent.length()) return true;
		char cFirst = scContent.charAt(0);
		
		if ('#'==cFirst) return true;
		
		if (m_context.subContext(iSpaceCount))
		{
			m_context = m_context.newContext(m_context, iSpaceCount);
			
			if (m_listContext.size()<=iSpaceCount)
				m_listContext.add(m_context);
			else
				m_listContext.set(iSpaceCount, m_context);
		}
		
		if (m_context.superContext(iSpaceCount))
		{
			if (0==iSpaceCount)
			{
				m_context = new ParseCategory(null, 0, ParseModifiers._NONE);
			}
			else
			{
				ParseContext contextParent = findContext(iSpaceCount-1);
				m_context = contextParent.newContext(contextParent, iSpaceCount);
			}
			
			m_listContext.set(iSpaceCount, m_context);
		}
		
		// Characters before indicate selections
		
		if (!Character.isLetterOrDigit(cFirst))
		{
			scContent = scContent.substring(1);
		}
		
		// Characters after indicate restrictions
		
		char cLast = scContent.charAt(scContent.length()-1);

		if (!Character.isLetterOrDigit(cLast))
		{
			scContent = scContent.substring(0, scContent.length()-1);
			scContent = scContent.trim();
		}
		
		if (!m_context.addNode(m_dataCharacter, cFirst, cLast, scContent))
		{
			return reportError(String.format("Could not add node %s", scContent));
		}
		
		if (!m_context.linkNode(m_dataCharacter, cFirst, cLast, scContent))
		{
			return reportError(String.format("Could not link node %s", scContent));
		}		
		
		return true;
	}
	
	private static final int _CATEGORY_SELECT_CATEGORY_SELECTION = -3; // Relative context of select that can set property of selection
	private static final int _SELECT_CATEGORY_SELECTION = -2; // Relative context of select that can set property of selection

	private enum ParseModifiers {_NONE, _SET_SUGGESTION, _SET_ANY, _SET_EXCLUSION, _SET_UNIQUE, _SET_CASCADE_TRUE, _SET_CASCADE_FALSE}
	
	private class ParseContext
	{
		protected ParseContext(ParseContext contextSuper, int iSpaceCount, ParseModifiers modifiersContext)
		{
			m_contextSuper = contextSuper;
			m_iSpaceCount = iSpaceCount;
			m_modifiersContext = modifiersContext;
		}
		
		protected ParseContext m_contextSuper = null;
		protected int m_iSpaceCount = 0;
		protected ParseModifiers m_modifiersContext;
		
		protected RegionData m_dataRegion = null;
		
		protected ParseContext createContext(String scType)
		{
			if (m_dataRegion.restoreContent(ParseHelper.this, scType, false, false, ""))
			{
				// First of check for subregions of the current data
				return m_context;
			}
			
			if (RegionData.sm_scIdentifier.equals(scType))
			{
				// New region data
				return new ParseContext(this, m_iSpaceCount+1, ParseModifiers._NONE);
			}
			
			if (CategoryData.sm_scIdentifier.equals(scType))
			{
				// New category data
				return new ParseCategory(this, m_iSpaceCount+1, ParseModifiers._NONE);
			}
			
			if (SelectionData.sm_scIdentifier.equals(scType))
			{
				// New selection data
				return new ParseSelection(this, m_iSpaceCount+1, ParseModifiers._NONE);
			}
			
			if (SelectionDataProxy.sm_scIdentifier.equals(scType))
			{
				// New proxy selection data
				return new ParseSelectionProxy(this, m_iSpaceCount+1, ParseModifiers._NONE);
			}
			
			return null;			
		}
		
		protected boolean populateContext(String scType, String scContent)
		{
			m_dataRegion = new RegionData(scContent);
			RegionData dataParent = m_contextSuper.m_dataRegion;
			dataParent.addRegion(m_dataNotify, m_dataRegion);
			return true;
		}
		
		protected boolean knownAttribute(String scType, String scAttribute, String scValue)
		{
			return true; // Default reject all attributes
		}
		
		protected ParseContext previousContext(String scType)
		{
			return m_contextSuper;
		}
		
		protected ParseContext newContext(ParseContext contextSuper, int iSpaceCount)
		{
			return null;
		}
		
		protected boolean subContext(int iSpaceCount)
		{
			return iSpaceCount > m_iSpaceCount;
		}		
		
		protected boolean superContext(int iSpaceCount)
		{
			return iSpaceCount < m_iSpaceCount;
		}

		protected boolean addNode(CharacterData dataGen, char cFirst, char cLast, String scLine) 
		{
			return false;
		}

		protected boolean linkNode(CharacterData charGenD_text, char cFirst, char cLast, String scContent) 
		{
			return false;
		}
		
		@Override
		public String toString()
		{
			if (null==m_dataRegion) return Integer.toString(m_iSpaceCount);
			return m_dataRegion.getName();
		}
	}
	
	private class ParseCharacter extends ParseContext
	{
		ParseCharacter(ParseContext contextSuper, int iSpaceCount, ParseModifiers modifiersContext)
		{
			super(contextSuper, iSpaceCount, modifiersContext);
		}
		
		@Override
		protected ParseContext createContext(String scType)
		{
			if (CharacterData.sm_scIdentifier==scType) return this; // Ignore this root
			if (null==m_dataRegion) return this; // First root region use this and populate
			return new ParseContext(this, 0, ParseModifiers._NONE);
		}
		
		@Override
		protected boolean populateContext(String scType, String scContent)
		{
			m_dataRegion = m_dataCharacter.getRoot();
			return true;
		}
		
		@Override
		protected ParseContext previousContext(String scType)
		{
			// There is no previous context
			return this;
		}
		
		@Override
		protected boolean superContext(int iSpaceCount)
		{
			return true;
		}		
	}
	
	private class ParseCategory extends ParseContext
	{
		ParseCategory(ParseContext contextSuper, int iSpaceCount, ParseModifiers modifiersContext)
		{
			super(contextSuper, iSpaceCount, modifiersContext);
		}
		
		private String m_scView = null;
		
		@Override
		protected ParseContext createContext(String scType)
		{
			if (m_dataRegion.restoreContent(ParseHelper.this, scType, false, false, ""))
			{
				// First check for subregions of the current data
				return this;
			}
			
			// Following cases if get data area immediately following
			
			if (RegionData.sm_scIdentifier.equals(scType))
			{
				// For region then next context is not the same region containing this category, but the parent of that region
				return m_contextSuper.m_contextSuper.createContext(scType);
			}
			
			if (CategoryData.sm_scIdentifier.equals(scType))
			{
				// For consecutive categories in the same region context is same region as this category
				return m_contextSuper.createContext(scType);
			}
			
			if (SelectionData.sm_scIdentifier.equals(scType))
			{
				// For selection add new context below this {using default nextContext}
				return super.createContext(scType);
			}
			
			if (SelectionDataProxy.sm_scIdentifier.equals(scType))
			{
				// For selection add new context below this {using default nextContext}
				return super.createContext(scType);
			}
			
			return null;
		}
		
		@Override
		protected boolean populateContext(String scType, String scContent)
		{
			m_dataRegion = m_dataCharacter.referenceCategory(scContent);
			RegionData dataParent = m_contextSuper.m_dataRegion;
			dataParent.addRegion(m_dataNotify, m_dataRegion);
			return true;
		}
		
		@Override
		protected ParseContext newContext(ParseContext contextSuper, int iSpaceCount)
		{
			return new ParseSelection(contextSuper, iSpaceCount, m_modifiersContext);
		}
		
		@Override
		protected boolean addNode(CharacterData dataGen, char cFirst, char cLast, String scLine) 
		{
			m_scCategory = scLine;
			
			// When category builds a selection it will mark the context as needing to be linked as suggestion
			if ('.'==cLast) m_modifiersContext = ParseModifiers._SET_SUGGESTION;
			if ('>'==cLast) m_modifiersContext = ParseModifiers._SET_EXCLUSION;
			if ('!'==cLast) m_modifiersContext = ParseModifiers._SET_UNIQUE;
			
			if ('\''==cLast)
			{
				// Use the 'path' to set the view
				int iIndex = scLine.indexOf('\'');
				m_scView = scLine.substring(iIndex+1);
				m_scCategory = scLine.substring(0, iIndex-1);
				return addCategoryWithView(m_scCategory, m_scView);				
			}
			
			if ('+'==cLast) m_modifiersContext = ParseModifiers._SET_CASCADE_TRUE;
			if ('-'==cLast) m_modifiersContext = ParseModifiers._SET_CASCADE_FALSE;
			
			return addCategory(m_scCategory);
		}
		
		@Override
		public boolean linkNode(CharacterData charGenD_text, char cFirst, char cLast, String scContent) 
		{
			return true;
		}

		String m_scCategory = null;
		
		public String getCategory() 
		{
			return m_scCategory;
		}
		
		public String getView()
		{
			return m_scView;
		}
	}
	
	private class ParseSelection extends ParseContext
	{
		public ParseSelection(ParseContext contextSuper, int iSpaceCount, ParseModifiers modifiersSelection) 
		{
			super(contextSuper, iSpaceCount, modifiersSelection);
			
			if (contextSuper instanceof ParseCategory)
				m_contextCategory = (ParseCategory)contextSuper;
			else
				m_contextCategory = ((ParseSelection)contextSuper).m_contextCategory;
		}

		private ParseCategory m_contextCategory = null;
		
		@Override
		protected boolean populateContext(String scType, String scContent)
		{
			CategoryData dataCategory = (CategoryData)m_contextSuper.m_dataRegion;
			m_dataRegion = requireExistingSelection(dataCategory, scContent);
			return true;
		}
		
		@Override
		protected boolean knownAttribute(String scType, String scAttribute, String scValue)
		{
			if (scAttribute=="selected" && true==Boolean.parseBoolean(scValue)) return true;
			// The attributes 'suggested' and 'excluded' are formed by other selections of this selection
			return false;
		}
		
		@Override
		protected ParseContext createContext(String scType)
		{
			if (m_dataRegion.restoreContent(ParseHelper.this, scType, false, false, ""))
			{
				// First of check for subregions of the current data - n_context could be set during restore
				return m_context;
			}
			
			if (RegionData.sm_scIdentifier.equals(scType))
			{
				// For region then next context is not the same region containing the category of this selection, but the parent of that region
				return m_contextSuper.m_contextSuper.m_contextSuper.createContext(scType);
			}
			
			if (CategoryData.sm_scIdentifier.equals(scType))
			{
				// If it is a category then add to region above category
				return m_contextSuper.m_contextSuper.createContext(scType);
			}
			
			if (SelectionData.sm_scIdentifier.equals(scType))
			{
				// For selection get category to add the selection
				return m_contextSuper.createContext(scType);
			}
			
			if (SelectionDataProxy.sm_scIdentifier.equals(scType))
			{
				// For selection get category to add the selection
				return m_contextSuper.createContext(scType);
			}
			
			return null;
		}
		
		@Override
		protected ParseContext newContext(ParseContext contextSuper, int iSpaceCount)
		{
			return new ParseCategory(contextSuper, iSpaceCount, ParseModifiers._NONE);
		}
		
		@Override
		protected boolean subContext(int iSpaceCount)
		{
			return iSpaceCount > m_iSpaceCount;
		}
		
		@Override
		protected boolean addNode(CharacterData dataGen, char cFirst, char cLast, String scLine) 
		{
			m_scSelection = scLine;
			String scView = null;
			
			if (m_scSelection.equals("Any"))
			{
				m_context.m_modifiersContext = ParseModifiers._SET_ANY;
				return true;
			}
			
			if ('^'==cLast)
			{
				// Remove any path used for popup
				int iIndex = scLine.indexOf('^');
				m_scSelection = scLine.substring(0, iIndex-1);
			}
			
			if ('~'==cLast)
			{
				// Remove any path used for popup
				int iIndex = scLine.indexOf('~');
				m_scSelection = scLine.substring(0, iIndex-1);
			}
			
			if ('='==cLast)
			{
				// Remove any math
				int iIndex = scLine.indexOf('=');
				m_scSelection = scLine.substring(0, iIndex-1);
			}
			
			if ('\''==cLast)
			{
				// Set display characteristics
				int iIndex = scLine.indexOf('\'');
				scView = scLine.substring(iIndex+1);
				m_scSelection = scLine.substring(0, iIndex-1);
			}			
			
			if ('\"'==cLast)
			{
				// Remove any value set
				int iIndex = scLine.indexOf('\"');
				m_scSelection = scLine.substring(0, iIndex-1);
			}
			
			if ('/'==cLast)
			{
				// Remove the subsequent characters from the selection name
				int iIndex = scLine.indexOf('/');
				m_scSelection = scLine.substring(0, iIndex-1);				
			}
			
			String scCategory = m_contextCategory.getCategory();
			String scCategoryView = m_contextCategory.getView();
			return addSelectionView(scCategory, scCategoryView, m_scSelection, scView);
		}
		
		@Override
		public boolean linkNode(CharacterData dataGen, char cFirst, char cLast, String scLine) 
		{
			ParseCategory contextCategory;
			ParseSelection contextSelection;
			
			if ('^'==cLast)
			{
				// Use the 'path' to set the view
				int iIndex = scLine.indexOf('^');
				String scView = scLine.substring(iIndex+1);
				return addSelectionPopup(m_contextCategory.getCategory(), m_scSelection, scView);
			}
			
			if ('~'==cLast)
			{
				// Use the 'path' to set the view
				int iIndex = scLine.indexOf('~');
				String scView = scLine.substring(iIndex+1);
				return addSelectionReplace(m_contextCategory.getCategory(), m_scSelection, scView);
			}
			
			if ('<'==cLast)
			{
				// Need the selection that is setting the category for this selection
				contextCategory = (ParseCategory)getRelativeContext(this, _CATEGORY_SELECT_CATEGORY_SELECTION);
				contextSelection = (ParseSelection)getRelativeContext(this, _SELECT_CATEGORY_SELECTION);
				return addSelectionPopback(contextCategory.m_scCategory, contextSelection.m_scSelection, m_contextCategory.getCategory(), m_scSelection);					
			}
			
			if ('&'==cLast)
			{
				return addSelectionFixed(m_contextCategory.getCategory(), m_scSelection, true);
			}
			
			if ('\"'==cLast)
			{
				int iIndex = scLine.indexOf('\"');
				return addSelectionValue(m_contextCategory.getCategory(), m_scSelection, scLine.substring(iIndex+1));
			}
			
			if ('='==cLast)
			{
				int iIndex = scLine.indexOf('=');
				return addSelectionMath(m_contextCategory.getCategory(), m_scSelection, scLine.substring(iIndex+1));
			}
			
			if ('*'==cLast)
			{
				return addSelectionToggle(m_contextCategory.getCategory(), m_scSelection, true);
			}
			
			if ('/'==cLast)
			{
				int iIndex = scLine.indexOf('/');
				return addSelectionInclude(m_contextCategory.getCategory(), m_scSelection, scLine.substring(iIndex+1));
			}
			
			boolean zAssumeCascadeTrue = true;
			
			switch (m_context.m_modifiersContext)
			{
			case _NONE:
				return true;
				
			case _SET_SUGGESTION:
				// Need the selection that is setting the category for this selection
				contextCategory = (ParseCategory)getRelativeContext(this, _CATEGORY_SELECT_CATEGORY_SELECTION);
				contextSelection = (ParseSelection)getRelativeContext(this, _SELECT_CATEGORY_SELECTION);
				return addSelectionSuggestion(contextCategory.m_scCategory, contextSelection.m_scSelection, m_contextCategory.getCategory(), m_scSelection);
				
			case _SET_ANY:
				contextCategory = (ParseCategory)getRelativeContext(this, _CATEGORY_SELECT_CATEGORY_SELECTION);
				contextSelection = (ParseSelection)getRelativeContext(this, _SELECT_CATEGORY_SELECTION);
				return addSelectionSuggestion(contextCategory.m_scCategory, contextSelection.m_scSelection, m_contextCategory.getCategory());
				
			case _SET_EXCLUSION:
				// Need the selection that is setting the category for this selection
				contextCategory = (ParseCategory)getRelativeContext(this, _CATEGORY_SELECT_CATEGORY_SELECTION);
				contextSelection = (ParseSelection)getRelativeContext(this, _SELECT_CATEGORY_SELECTION);
				return addSelectionExclusion(contextCategory.m_scCategory, contextSelection.m_scSelection, m_contextCategory.getCategory(), m_scSelection);
				
			case _SET_UNIQUE:
				return addSelectionUnique(m_contextCategory.getCategory(), m_scSelection);
				
			case _SET_CASCADE_FALSE:
				zAssumeCascadeTrue = false;
			case _SET_CASCADE_TRUE:
				contextCategory = (ParseCategory)getRelativeContext(this, _CATEGORY_SELECT_CATEGORY_SELECTION);
				contextSelection = (ParseSelection)getRelativeContext(this, _SELECT_CATEGORY_SELECTION);
				return addSelectionCascade(contextCategory.m_scCategory, contextSelection.m_scSelection, m_contextCategory.getCategory(), m_scSelection, zAssumeCascadeTrue);
			}
			
			return false;
		}

		private String m_scSelection = null;
	}
	
	private class ParseSelectionProxy extends ParseSelection
	{
		public ParseSelectionProxy(ParseContext contextSuper, int iSpaceCount, ParseModifiers modifiersSelection) 
		{
			super(contextSuper, iSpaceCount, modifiersSelection);
		}

		@Override
		protected boolean populateContext(String scType, String scContent)
		{
			CategoryData dataCategory = (CategoryData)m_contextSuper.m_dataRegion;

			int iIndex = scContent.indexOf('_');
			// Must have at least 1 character for the name
			if (1>iIndex) return false;
			
			String scName = scContent.substring(0, iIndex);
			String scView = scContent.substring(iIndex);				
			
			// Prevent double '_'
			if (1<scView.length()) for (; '_'==scView.charAt(1); scView = scView.substring(1));
			
			if (scView.startsWith("_graphic_"))
			{
				scName = scName + "_graphic";
				scView = scView.substring(9);
				if (0==scView.length()) return super.populateContext(scType, scName);
			}
			
			m_dataRegion = referenceSelection(dataCategory, scName, scView);
			
			return true;
		}
		
		@Override
		protected ParseContext createContext(String scType)
		{
			if (m_dataRegion.restoreContent(ParseHelper.this, scType, false, false, ""))
			{
				// First of check for subregions of the current data - n_context could be set during restore
				return m_context;
			}
			
			if (RegionData.sm_scIdentifier.equals(scType))
			{
				// For region then next context is not the same region containing the category of this selection, but the parent of that region
				return m_contextSuper.m_contextSuper.m_contextSuper.createContext(scType);
			}
			
			if (CategoryData.sm_scIdentifier.equals(scType))
			{
				// If it is a category then add to region above category
				return m_contextSuper.m_contextSuper.createContext(scType);
			}
			
			if (SelectionData.sm_scIdentifier.equals(scType))
			{
				// For selection get category to add the selection
				return m_contextSuper.createContext(scType);
			}
			
			if (SelectionDataProxy.sm_scIdentifier.equals(scType))
			{
				// For selection get category to add the selection
				return m_contextSuper.createContext(scType);
			}
			
			return null;
		}
	}
	
	private class ParseLocal extends ParseContext
	{
		ParseLocal(ParseContext contextSuper, CharacterHelper helperParse)
		{
			super(contextSuper, 0, ParseModifiers._NONE);
			m_helperParse = helperParse;
		}
		
		private CharacterHelper m_helperParse = null;
		
		@Override
		protected ParseContext createContext(String scType)
		{
			if (m_helperParse.restoreContent(ParseHelper.this, scType, true, false, null))
			{
				// Context can be changed by helper creating local
				return m_context;
			}
			
			return super.createContext(scType);			
		}
		
		@Override
		protected boolean populateContext(String scType, String scContent)
		{
			if (m_helperParse.restoreContent(ParseHelper.this, scType, false, false, scContent))
			{
				return true;
			}
			
			return super.populateContext(scType, scContent);
		}
		
		@Override
		protected ParseContext previousContext(String scType)
		{
			if (m_helperParse.restoreContent(ParseHelper.this, scType, false, true, null))
			{
				// Change back to local from parse local helper changes back from local explicitly or implicitly by returning false
				return m_context;
			}
			
			return m_contextSuper;
		}		
	}
	
	public boolean startParseLocal(CharacterHelper helperParse)
	{
		// Raise to new local 
		m_context = new ParseLocal(m_context, helperParse);
		m_context.m_dataRegion = new RegionData("Local");
		return true;
	}
	
	public RegionData dataParseLocal(int iChild)
	{
		// Local context has a RegionDataRoot but works for other contexts too
		RegionData dataTree = m_context.m_dataRegion;
		RegionData dataReturn = (RegionData)dataTree.getChildAt(iChild);
		dataTree.remove(iChild);
		return dataReturn;
	}
	
	public boolean endParseLocal()
	{
		// Drop from first local element back to local
		m_context = m_context.m_contextSuper;
		return true;
	}
	
	public CharacterData requireExitingCharacter()
	{
		return m_dataCharacter;
	}
	
	public CategoryData requireExistingCategory(String scCategory)
	{
		CategoryData dataCategory = m_dataCharacter.getCategory(scCategory);
		
		if (null==dataCategory)
		{
			dataCategory = m_dataCharacter.baseCategory(scCategory);
		}
		
		return dataCategory;		
	}
	
	public CategoryData referenceCategory(String scCategory)
	{
		// Create a proxy to the category
		return m_dataCharacter.referenceCategory(scCategory);
	}
	
	public SelectionData requireExistingSelection(CategoryData dataCategory, String scSelection)
	{
		SelectionData dataSelection = (SelectionData)dataCategory.getRegion(scSelection);
		
		if (null==dataSelection)
		{
			dataSelection = new SelectionData(scSelection);
			dataCategory.addRegionWithView(m_dataNotify, null, dataSelection);
		}
		
		return dataSelection;
	}	
	
	public SelectionData referenceSelection(CategoryData dataCategory, String scSelection, String scView)
	{
		// Create a proxy to the selection
		return m_dataCharacter.referenceSelection(dataCategory, scSelection, scView);
	}
	
	private ParseContext getRelativeContext(ParseContext contextFrom, int selectCategorySelection) 
	{
		int iIndex = m_listContext.indexOf(contextFrom);
		return findContext(iIndex + selectCategorySelection);
	}

	private ParseContext findContext(int iIndex)
	{
		if (m_listContext.size()<=iIndex) throw new RuntimeException("Super context out of range");
		return m_listContext.get(iIndex);
	}

	private int countSpaces(String scLine)
	{
		int iCount = 0;
		for (int iIndex = 0, iLength = scLine.length(); iIndex<iLength; ++iIndex)
		{
			if (' '==scLine.charAt(iIndex)) 
				iCount += 1;
			else
				break;
		}
		return iCount;
	}
	
	public boolean reportError(String scError)
	{
		RegionData dataRegion = m_dataCharacter.getRoot();
		return dataRegion.fireCharacterEvent(new CharacterEventError(scError));
	}
	
	private boolean addCategory(String scCategory)
	{
		if (m_dataCharacter.hasCategory(scCategory)) return true;
		m_dataCharacter.baseCategory(scCategory);
		return true;
	}
	
	private boolean addCategoryWithView(String scCategory, String scView)
	{
		CategoryData dataCategory = m_dataCharacter.referenceCategory(scCategory);
		if (null==dataCategory) return reportError(String.format("Could not add view '%s' because no category '%s'", scView, scCategory));

		CategoryDataProxy proxyCategory = (CategoryDataProxy)dataCategory; // new CategoryDataProxy(dataCategory);
		RegionData dataRegion = m_dataCharacter.getRoot();
		return dataRegion.addRegionWithView(m_dataNotify, scView, proxyCategory);
	}
	
	private boolean addSelection(String scCategory, String scSelection)
	{
		CategoryData dataCategory = m_dataCharacter.getCategory(scCategory);
		if (null==dataCategory) return reportError(String.format("Could not add selection '%s' because could not find category '%s'", scSelection, scCategory));
		
		SelectionData dataSelection = new SelectionData(scSelection);
		dataCategory.addRegionWithView(m_dataNotify, null, dataSelection);
		return true;
	}
	
	private boolean addSelectionView(String scCategory, String scCategoryView, String scSelection, String scSelectionView)
	{
		if (!addSelection(scCategory, scSelection)) return false; // Already raised event
		
		// If no view nothing else to do
		if (null==scSelectionView) return true;

		// Find existing selection
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategory, scSelection)) return reportError(String.format("Could not add fixed selection because could not find %s.%s", scSelection, scCategory));
		SelectionData dataSelection = referenceSelection.existingSelection();
		
		// Create proxy for selection
		SelectionDataProxy dataSelectionProxy = new SelectionDataProxy(dataSelection, scSelectionView);
		
		RegionData dataRegion = m_dataCharacter.getRoot();
		RegionData dataCategoryView = dataRegion.getRegion(scCategoryView);
		CategoryDataProxy dataCategory = (CategoryDataProxy)dataCategoryView.getRegion(scCategory);
		
		dataCategory.addRegion(m_dataNotify, dataSelectionProxy);
		
		return true;
	}
	
	private class ReferenceSelection
	{
		private CategoryData m_dataCategory = null;
		private SelectionData m_dataSelection = null;
		
		private boolean checkSelectionExists(String scCategory, String scSelection)
		{
			m_dataCategory = m_dataCharacter.getCategory(scCategory);
			if (null==m_dataCategory) return false;		
			m_dataSelection = (SelectionData)m_dataCategory.getRegion(scSelection);
			if (null==m_dataSelection) return false;		
			return true;			
		}
		
		private SelectionData existingSelection()
		{
			return m_dataSelection;
		}
	}

	private boolean addSelectionSuggestion(String scCategoryFrom, String scSelectionFrom, String scCategoryTo, String scSelectionTo) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategoryFrom, scSelectionFrom)) return reportError(String.format("Could not add suggestion because could not find %s.%s", scSelectionFrom, scCategoryFrom));
		SelectionData dataSelectionFrom = referenceSelection.existingSelection();
		
		if (!referenceSelection.checkSelectionExists(scCategoryTo, scSelectionTo)) return reportError(String.format("Could not add suggestion because could not find %s.%s", scSelectionTo, scCategoryTo));		
		SelectionData dataSelectionTo = referenceSelection.existingSelection();
		
		return dataSelectionFrom.suggestSelection(dataSelectionTo);
	}

	private boolean addSelectionSuggestion(String scCategoryFrom, String scSelectionFrom, String scCategoryTo)
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategoryFrom, scSelectionFrom)) return reportError(String.format("Could not add suggestions because could not find %s.%s", scSelectionFrom, scCategoryFrom));
		SelectionData dataSelectionFrom = referenceSelection.existingSelection();
		
		CategoryData dataCategoryTo = m_dataCharacter.getCategory(scCategoryTo);
		if (null==dataCategoryTo) return reportError(String.format("Could not add suggestions for %s because could not find %s", scSelectionFrom, scCategoryTo));
		
		return dataSelectionFrom.suggestCategory(dataCategoryTo);
	}
	
	private boolean addSelectionExclusion(String scCategoryFrom, String scSelectionFrom, String scCategoryTo, String scSelectionTo) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategoryFrom, scSelectionFrom)) return reportError(String.format("Could not add exclusion because could not find %s.%s", scSelectionFrom, scCategoryFrom));
		SelectionData dataSelectionFrom = referenceSelection.existingSelection();
		
		if (!referenceSelection.checkSelectionExists(scCategoryTo, scSelectionTo)) return reportError(String.format("Could not add exclusion because could not find %s.%s", scSelectionTo, scCategoryTo));
		SelectionData dataSelectionTo = referenceSelection.existingSelection();
		
		return dataSelectionFrom.excludeSelection(dataSelectionTo);
	}
	
	private boolean addSelectionPopup(String scCategory, String scSelection, String scView)
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategory, scSelection)) return reportError(String.format("Could not add popup because could not find %s.%s", scSelection, scCategory));
		SelectionData dataSelection = referenceSelection.existingSelection();
		
		String scCategoryPopup = dataSelection.getLast(scView);
		CategoryData dataCategoryPopup = m_dataCharacter.baseCategory(scCategoryPopup);

		String scRegionPopup = dataSelection.getPath(scView);
		if (!dataSelection.addRegionWithView(m_dataNotify, scRegionPopup, dataCategoryPopup)) return reportError(String.format("Could not add %s popup region %s", scSelection, scView));
		
		String scRoot = dataSelection.getFirst(scView, dataCategoryPopup);
		RegionData dataRegionRoot = dataSelection.getRegion(scRoot);
		if (null==dataRegionRoot) return reportError(String.format("Did not build %s popup %s", scSelection, scView));
		
		return dataSelection.popupCategory(dataRegionRoot); 		
	}
	
	private boolean addSelectionReplace(String scCategory, String scSelection, String scView)
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategory, scSelection)) return reportError(String.format("Could not add popup replace because could not find %s.%s", scSelection, scCategory));
		SelectionData dataSelection = referenceSelection.existingSelection();
				
		RegionData dataRoot = m_dataCharacter.getRoot();
		String scRegionPath = dataRoot.getPath(scView);
		
		RegionData dataRegion = dataRoot.getRegion(scRegionPath);
		
		if (null==dataRegion)
		{
			String scRegionLast = dataRoot.getLast(scRegionPath);
			dataRegion = new RegionData(scRegionLast);
			String scRegionView = dataRoot.getPath(scRegionPath);		
			dataRoot.addRegionWithView(m_dataNotify, scRegionView, dataRegion);
			// Obtain region again to move through tree
			dataRegion = dataRoot.getRegion(scRegionPath);
		}
		
		String scCategoryReplacing = dataRoot.getLast(scView);
		// Build a proxy in case the category is used elsewhere
		//CategoryData dataCategoryReplace = m_dataCharacter.baseCategory(scCategoryReplacing);
		CategoryData dataCategoryReplace = m_dataCharacter.referenceCategory(scCategoryReplacing);

		return dataSelection.replaceCategory(dataRegion, dataCategoryReplace); 		
	}
	
	private boolean addSelectionPopback(String scCategoryFrom, String scSelectionFrom, String scCategoryTo, String scSelectionTo) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategoryFrom, scSelectionFrom)) return reportError(String.format("Could not add popback because could not find %s.%s", scSelectionFrom, scCategoryFrom));
		SelectionData dataSelectionFrom = referenceSelection.existingSelection();
		
		if (!referenceSelection.checkSelectionExists(scCategoryTo, scSelectionTo)) return reportError(String.format("Could not add popback because could not find %s.%s", scSelectionTo, scCategoryTo));
		SelectionData dataSelectionTo = referenceSelection.existingSelection();
		
		return dataSelectionTo.popbackSelection(dataSelectionFrom);
	}
	
	private boolean addSelectionFixed(String scCategory, String scSelection, boolean zSelected) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategory, scSelection)) return reportError(String.format("Could not add fixed selection because could not find %s.%s", scSelection, scCategory));
		SelectionData dataSelection = referenceSelection.existingSelection();
		
		return dataSelection.fixedState(m_dataCharacter.getRoot(), zSelected);
	}
	
	private boolean addSelectionUnique(String scCategory, String scSelection) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategory, scSelection)) return reportError(String.format("Could not add unique selection because could not find %s.%s", scSelection, scCategory));
		SelectionData dataSelection = referenceSelection.existingSelection();
		
		return dataSelection.uniqueSelection();
	}
	
	private boolean addSelectionValue(String scCategory, String scSelection, String scValue) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategory, scSelection)) return reportError(String.format("Could not add value because could not find %s.%s", scSelection, scCategory));
		SelectionData dataSelection = referenceSelection.existingSelection();
		
		ValueDataText dataValue = new ValueDataText(scValue);
		Object valueSet = dataSelection.setData(dataValue);
		return null!=valueSet;
	}
	
	private boolean addSelectionMath(String scCategory, String scSelection, String scValue) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategory, scSelection)) return reportError(String.format("Could not add math because could not find %s.%s", scSelection, scCategory));
		SelectionData dataSelection = referenceSelection.existingSelection();
		
		EvaluationHelper helperEvaluation = m_dataCharacter.getEvaluation();
		
		// Check for assignment based on selection ( = destination = expression = ) - first and last removed
		
		int iAssignment = scValue.indexOf('=');
		
		if (0<iAssignment)
		{
			String scDestination = scValue.substring(0, iAssignment);
			SelectionData dataSelectionValue = helperEvaluation.referenceSelection(scDestination);
			String scExpression = scValue.substring(iAssignment+1);
			return dataSelection.mathValue(dataSelectionValue, helperEvaluation, scExpression);
		}
		
		// Assignment fixed for selection ( = expression = )
		
		ValueDataMath dataValue = new ValueDataMath(helperEvaluation, scValue);
		Object valueSet = dataSelection.setData(dataValue);
		return null!=valueSet;
	}
	
	private boolean addSelectionCascade(String scCategoryFrom, String scSelectionFrom, String scCategoryTo, String scSelectionTo, boolean zState) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategoryFrom, scSelectionFrom)) return reportError(String.format("Could not add suggestion because could not find %s.%s", scSelectionFrom, scCategoryFrom));
		SelectionData dataSelectionFrom = referenceSelection.existingSelection();
		
		if (!referenceSelection.checkSelectionExists(scCategoryTo, scSelectionTo)) return reportError(String.format("Could not add suggestion because could not find %s.%s", scSelectionTo, scCategoryTo));		
		SelectionData dataSelectionTo = referenceSelection.existingSelection();
		
		return dataSelectionFrom.cascadeSelection(dataSelectionTo, zState);
	}

	private boolean addSelectionToggle(String scCategory, String scSelection, boolean zSelected) 
	{
		ReferenceSelection referenceSelection = new ReferenceSelection();
		if (!referenceSelection.checkSelectionExists(scCategory, scSelection)) return reportError(String.format("Could not add fixed selection because could not find %s.%s", scSelection, scCategory));
		SelectionData dataSelection = referenceSelection.existingSelection();
		
		return dataSelection.toggleSelection(m_dataCharacter.getRoot(), zSelected);
	}
	
	private boolean addSelectionInclude(String scCategory, String scSelection, String scIncludeFileName)
	{
		CharacterEventRecurse eventEdit = new CharacterEventRecurse(m_dataCharacter, scIncludeFileName);
		//System.out.println("Loading file "+scIncludeFileName);
		return m_dataNotify.fireCharacterEvent(eventEdit);
	}	
}
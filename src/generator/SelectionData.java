package generator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.tree.TreeNode;

import generator.CharacterEvent.EventSelectEnum;

class SelectionData extends RegionData
{
	private static final long serialVersionUID = -4188626480624999136L;
	public static final String sm_scIdentifier = "SELECT";

	public SelectionData(String scName)
	{
		super(scName);
		
		m_dataValue = new ValueData();
		
		m_actionSelect = new SelectionDataAction();
		m_zSelected = false;
		
		m_zSuggested = false;
		m_listSuggested = new ArrayList<SelectionData>();
		
		m_zExcluded = false;
		m_listExcluded = new ArrayList<SelectionData>();
	}
	
	private SelectionDataAction m_actionSelect = null;
	private boolean m_zSelected;	
	
	private ValueData m_dataValue = null;

	private boolean m_zSuggested;
	private List<SelectionData> m_listSuggested = null;

	private boolean m_zExcluded;
	private List<SelectionData> m_listExcluded = null;
	
	@Override
	public CategoryData getParent()
	{
		return (CategoryData)super.getParent();
	}
	
	@Override
	protected boolean removeThis(RegionData dataParent)
	{
		// Keep the node parent for sending events
		setParent(dataParent);
		return super.removeThis(dataParent);
	}
		
	public boolean setSelected(NotifyData dataEvent, boolean zSelected) 
	{
		if (zSelected==m_zSelected) return true;
		return setSelected(new SelectionDataEvent(dataEvent, zSelected));
	}
	
	protected boolean setSelected(SelectionDataEvent eventSelection) 
	{
		if (!m_actionSelect.selectAction(this, eventSelection)) return eventSelection.fireErrorEvent(String.format("Selection of %s failed", getName()));
		return eventSelection.fireCharacterEvent(new CharacterEventSelect(EventSelectEnum._CHANGE_SELECTED, this, m_zSelected));
	}
	
	protected boolean setSelected(boolean zSelected)
	{
		m_zSelected = zSelected;
		return m_zSelected;
	}
	
	public boolean isSelected()
	{
		return m_zSelected;
	}
		
	
	public Object getValue()
	{
		Object valueReturn = m_dataValue.getValue(this);
		
		// Get the current value and fire event if it has changed		
		if (m_dataValue.sameValue()) return valueReturn;
		
		// Event is generated when data has changed
		CharacterEventValue eventValue = new CharacterEventValue(EventSelectEnum._CHANGE_VALUE, this, m_dataValue);
		// Event is always fired from the root since most listeners attached there
		fireCharacterEvent(eventValue);
		
		return valueReturn;
	}
	
	public ValueData getData()
	{
		return m_dataValue;
	}
	
	public boolean sameSelection(SelectionData dataCheck)
	{
		return this==dataCheck;
	}
	
	public Object setData(ValueData dataValue)
	{
		m_dataValue = m_dataValue.updateValue(dataValue);
		// Event is generated on get where the actual value of the selection is calculated
		return getValue();
	}
	
	public boolean setSuggestion(SelectionDataEvent eventSelect, SelectionData dataSelected) 
	{
		if (eventSelect.isSelect())
		{
			if (m_listSuggested.contains(dataSelected)) return true;
			m_listSuggested.add(dataSelected);
			m_zSuggested = true;
		}
		else
		{
			if (!m_listSuggested.contains(dataSelected)) return true;
			m_listSuggested.remove(dataSelected);
			if (0<m_listSuggested.size()) return true; 
			m_zSuggested = false;
		}
		
		return eventSelect.fireCharacterEvent(new CharacterEventSelect(EventSelectEnum._SET_SUGGESTION, this, m_zSuggested));
	}
	
	public boolean isSuggested()
	{
		return m_zSuggested;
	}
	

	public boolean setExclusion(SelectionDataEvent eventSelect, SelectionData dataSelected) 
	{
		if (eventSelect.isSelect())
		{
			if (m_listExcluded.contains(dataSelected)) return true;
			m_listExcluded.add(dataSelected);
			m_zExcluded = true;
		}
		else
		{
			if (!m_listExcluded.contains(dataSelected)) return true;
			m_listExcluded.remove(dataSelected);
			if (!m_listExcluded.isEmpty()) return true; 
			m_zExcluded = false;
		}
		return eventSelect.fireCharacterEvent(new CharacterEventSelect(EventSelectEnum._SET_EXCLUSION, this, m_zExcluded));
	}
	
	public boolean isExcluded()
	{
		return m_zExcluded;
	}
	
	public Iterator<SelectionData> getExclusions()
	{
		return m_listExcluded.iterator();
	}
	
	
	public SelectionDataAction getAction()
	{
		return m_actionSelect;
	}
	
	public boolean setAction(SelectionDataAction actionSelect)
	{
		m_actionSelect = actionSelect;
		return true;
	}
	
	public boolean suggestSelection(SelectionData dataSelectionTo) 
	{
		return SelectionDataAction_suggestSelection.createAction(this, dataSelectionTo);
	}
	
	public boolean suggestCategory(CategoryData dataCategoryTo) 
	{
		return SelectionDataAction_suggestCategory.createAction(this, dataCategoryTo);
	}
	
	public boolean excludeSelection(SelectionData dataSelectionTo) 
	{
		return SelectionDataAction_exclude.createAction(this, dataSelectionTo);
	}
	
	public boolean popupCategory(RegionData dataRegion)
	{
		for (SelectionDataAction actionSelect = m_actionSelect; null!=actionSelect; actionSelect = actionSelect.next())
		{
			if (!(actionSelect instanceof SelectionDataAction_popup)) continue;
			SelectionDataAction_popup actionSelectPopup = (SelectionDataAction_popup)actionSelect;
			if (actionSelectPopup.hasRegion(dataRegion.getName())) return true;
		}
		return SelectionDataAction_popup.createAction(this, dataRegion);
	}
	
	public boolean popbackSelection(SelectionData dataSelection)
	{
		return SelectionDataAction_popback.createAction(this, dataSelection);
	}
	
	public boolean removableSelection(SelectionData dataSelection)
	{
		// Only used within selection actions to remove action
		return SelectionDataAction_remove.createAction(this, dataSelection);
	}
	
	public boolean fixedState(RegionData dataNotify, boolean zFixed)
	{
		setSelected(dataNotify, zFixed);
		return SelectionDataAction_fixed.createAction(this, zFixed);
	}
	
	public boolean uniqueSelection()
	{
		return SelectionDataAction_unique.createAction(this, this);
	}
	
	public boolean replaceCategory(RegionData dataRegion, CategoryData dataCategory)
	{
		//m_actionSelect = new SelectionDataAction_replace(dataRegion, dataCategory);
		return SelectionDataAction_substitute.createAction(this, dataRegion, dataCategory);
	}
	
	public boolean mathValue(SelectionData dataSelection, EvaluationHelper helperEvaluation, String scExpression)
	{
		return SelectionDataAction_math.createAction(this, dataSelection, helperEvaluation, scExpression);
	}
	
	public boolean cascadeSelection(SelectionData dataSelection, boolean zState) 
	{
		return SelectionDataAction_cascade.createAction(this, dataSelection, zState);
	}
	
	public boolean toggleSelection(RegionData dataNotify, boolean zToggle)
	{
		setSelected(dataNotify, !zToggle);
		return SelectionDataAction_toggle.createAction(this, zToggle);		
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleSelection(getName(), this, sm_scIdentifier);
	}
	
	@Override
	public boolean persistContent(PersistHelper helperPersist) 
	{
		for (ValueData dataValue = getData(); null!=dataValue; dataValue = dataValue.getNextValue())
		{
			dataValue.persistData(helperPersist);
		}
		
		for (SelectionDataAction actionSelect = getAction(); null!=actionSelect; actionSelect = actionSelect.next())
		{
			// Persist selection actions
			actionSelect.persistData(helperPersist);
		}

		return true;
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		// If the new node is an action or value then return a new selection class
		
		if (SelectionDataAction_cascade.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_cascade();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}
		
		if (SelectionDataAction_exclude.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_exclude();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}

		if (SelectionDataAction_fixed.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_fixed();
			return helperParse.startParseLocal(m_actionSelect);
		}

		if (SelectionDataAction_math.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_math();
			return helperParse.startParseLocal(m_actionSelect);
		}

		if (SelectionDataAction_popback.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_popback();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}

		if (SelectionDataAction_popup.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_popup();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}

		if (SelectionDataAction_replace.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_replace();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}
		
		if (SelectionDataAction_substitute.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_substitute();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}
		
		if (SelectionDataAction_suggestCategory.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_suggestCategory();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}
		
		if (SelectionDataAction_suggestSelection.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_suggestSelection();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}
		
		if (SelectionDataAction_toggle.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_toggle();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}
		
		if (SelectionDataAction_unique.sm_scIdentifier==scType)
		{
			m_actionSelect = new SelectionDataAction_unique();
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(m_actionSelect);
		}
		
		if (ValueDataMath.sm_scIdentifier==scType)
		{
			CharacterData dataCharacter = helperParse.requireExitingCharacter();
			EvaluationHelper helperEvaluation = dataCharacter.getEvaluation();
			ValueDataMath value = new ValueDataMath(helperEvaluation, scContent);
			setData(value);
			// Parse helper uses local context to drive data through to action
			return helperParse.startParseLocal(value);
		}
		
		if (ValueDataText.sm_scIdentifier==scType)
		{
			ValueDataText value = new ValueDataText(scContent);
			setData(value);
			return helperParse.startParseLocal(value);
		}
		
		return false;
	}
	
	@Override
	public boolean handleView(ViewHelper helperView, JComponent componentView) 
	{
		return helperView.buildPanel(this, null, componentView);
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData dataSelectionMerge = (SelectionData)dataRegionMerge;
		
		// Merge regions
		if (!super.handleMerge(dataRegionParent, helperMerge, dataCharacterMerge, dataRegionMerge)) return false;
		
		// Merge value
		ValueData valueMerge = dataSelectionMerge.m_dataValue;
		m_dataValue = valueMerge.mergeValue();
		
		// Merge actions
		SelectionDataAction actionSelectMerge = dataSelectionMerge.getAction();		
		dataSelectionMerge.setAction(new SelectionDataAction());
		
		for (; null!=actionSelectMerge; actionSelectMerge = actionSelectMerge.next())
		{
			actionSelectMerge.handleMerge(this, helperMerge, dataCharacterMerge, dataRegionMerge);
		}
		
		return true;
	}
	
	public String getState()
	{
		StringBuffer sb = new StringBuffer();
		
		if (isSelected())
			sb.append("SELECTED ");
		else
			sb.append("deselected ");
		
		if (isExcluded()) sb.append("- EXCLUDED ");
		
		if (isSuggested()) sb.append("- SUGGESTED ");

		sb.append("Value [");
		sb.append(getValue().toString());
		sb.append("]\n");
		
		sb.append("From ");
		for(ValueData valueSelect = m_dataValue;
				null!=valueSelect;
				valueSelect = valueSelect.getNextValue())
		{
			sb.append('{');
			sb.append(valueSelect.toString());
			sb.append("}\n");
		}
		
		sb.append("Actions ");
		for(SelectionDataAction actionSelect = m_actionSelect;
				null!=actionSelect;
				actionSelect = actionSelect.next())
		{
			sb.append('<');
			sb.append(actionSelect.toString());
			sb.append(">\n");
		}

		return sb.toString();
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(parent.toString()); // Selection should always have a parent
		sb.append('.');
		sb.append(getName());
		sb.append(" [");
		sb.append(m_dataValue.toString());
		sb.append("] ");
		return sb.toString();
	}
}

class SelectionDataProxy extends SelectionData
{
	private static final long serialVersionUID = -4605132186450417216L;
	public static final String sm_scIdentifier = "SELECT_PROXY";

	public SelectionDataProxy(SelectionData dataSelection, String scView) 
	{
		super(String.format("%s_%s", dataSelection.getName(), scView));
		m_dataSelection = dataSelection;
		m_scView = scView;
	}
	
	private SelectionData m_dataSelection = null;
	private String m_scView = null;

	public SelectionData getSource() 
	{
		return m_dataSelection;
	}

	public String getView() 
	{
		return m_scView;
	}

	@Override
	public boolean setSelected(NotifyData dataNotify, boolean zSelected) 
	{
		return m_dataSelection.setSelected(dataNotify, zSelected);
	}

	@Override
	public boolean isSelected() 
	{
		return m_dataSelection.isSelected();
	}

	@Override
	public Object getValue() 
	{
		return m_dataSelection.getValue();
	}

	@Override
	public ValueData getData() 
	{
		return m_dataSelection.getData();
	}
	
	@Override
	public boolean sameSelection(SelectionData dataCheck)
	{
		return m_dataSelection.sameSelection(dataCheck);
	}
	
	@Override
	public Object setData(ValueData dataValue) 
	{
		return m_dataSelection.setData(dataValue);
	}

	@Override
	public boolean setSuggestion(SelectionDataEvent eventSelect, SelectionData dataSelected) 
	{
		return m_dataSelection.setSuggestion(eventSelect, dataSelected);
	}

	@Override
	public boolean isSuggested() 
	{
		return m_dataSelection.isSuggested();
	}

	@Override
	public boolean setExclusion(SelectionDataEvent eventSelect, SelectionData dataSelected) 
	{
		return m_dataSelection.setExclusion(eventSelect, dataSelected);
	}

	@Override
	public boolean isExcluded() 
	{
		return m_dataSelection.isExcluded();
	}

	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleSelection(getName(), m_dataSelection, sm_scIdentifier);
	}

	@Override
	public boolean handleView(ViewHelper helperView, JComponent componentView) 
	{
		return helperView.buildPanel(this, null, componentView);
	}

	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		return m_dataSelection.handleMerge(this, helperMerge, dataCharacterMerge, dataRegionMerge);
	}

	@Override
	public String getState() 
	{
		StringBuilder sb = new StringBuilder();
		sb.append("View of ");
		sb.append(m_dataSelection.getState());
		return sb.toString();
	}
}

class ValueData implements CharacterHelper, Serializable
{
	private static final long serialVersionUID = 2474875784994510643L;

	public ValueData()
	{
	}

	private boolean m_zSameValue = true;
	private ValueData m_dataValueNext = null;
	
	public Object getValue(SelectionData dataSelection)
	{
		return dataSelection.getName();
	}
	
	public boolean sameValue()
	{
		boolean zSameValue = m_zSameValue;
		m_zSameValue = true;
		return zSameValue;
	}

	public ValueData updateValue(ValueData dataValue) 
	{
		// When the value for the selection changes determine what to do with the value
		// Simple case is to replace existing with new value
		dataValue.m_zSameValue = false;
		return dataValue;
	}

	public void setNextValue(ValueData dataNext)
	{
		m_dataValueNext = dataNext;
	}
	
	public ValueData getNextValue()
	{
		return m_dataValueNext;
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		// No specific data to persist
		return true;
	}

	@Override
	public boolean persistContent(PersistHelper helperPersist) 
	{
		return false;
	}

	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent) 
	{
		return false;
	}

	@Override
	public boolean handleView(ViewHelper helperView, JComponent componentView) 
	{
		// No specific value view
		return false;
	}

	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		// Nothing to merge
		return false;
	}
	
	public ValueData mergeValue()
	{
		return this;
	}
	
	@Override
	public String toString()
	{
		if (null==m_dataValueNext) return ". ";
		StringBuilder sb = new StringBuilder();
		sb.append(m_dataValueNext.toString());
		sb.append("and ");
		return sb.toString();
	}
}

class ValueDataText extends ValueData
{
	private static final long serialVersionUID = 3041037607591978714L;
	public final static String sm_scIdentifier = "VALUE_TEXT";
	
	public ValueDataText(String scText)
	{
		m_scText = scText;
	}
	
	protected String m_scText = null;
	
	@Override
	public Object getValue(SelectionData dataSelection)
	{
		return m_scText;
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleElement(sm_scIdentifier, m_scText);
	}

	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent) 
	{
		if (zEnd) return false;
		m_scText = scContent;
		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("{");
		sb.append(m_scText);
		sb.append("} ");
		return sb.toString();
	}
}

class ValueDataMath extends ValueDataText
{
	private static final long serialVersionUID = -2529587220442932805L;
	public final static String sm_scIdentifier = "VALUE_MATH";
	
	public ValueDataMath(EvaluationHelper helperEvaluation, String scExpression)
	{
		super(scExpression);
		m_helperEvaluation = helperEvaluation;
		// Need to use something so that first evaluation of value is seen as 'new'
		m_Value = "";
	}
	
	private transient EvaluationHelper m_helperEvaluation = null;
	private transient Object m_Value = null;
	private transient boolean m_zCalculating = false;
	
	private void readObject(ObjectInputStream streamIn) throws IOException, ClassNotFoundException
	{
		streamIn.defaultReadObject();
		m_helperEvaluation = EvaluationHelper.currentEvaluationHelper();
		return;
	}
		
	public String getExpression()
	{
		return m_scText;
	}
	
	public ValueData setExpression(String scExpression)
	{
		m_scText = scExpression;
		return this;
	}

	// Get value including chaining of previous data values
	@Override
	public Object getValue(SelectionData dataSelection)
	{
		if (!m_zCalculating)
		{
			m_zCalculating = true;
			m_Value = evaluateValue(dataSelection);
			if (m_Value instanceof String)
			{
				System.err.println(toString());
			}
			else
			{
				System.out.println(toString());				
			}
			m_zCalculating = false;
		}
		return m_Value;		
	}
	
	private Object evaluateValue(SelectionData dataSelection)
	{
		String scBefore = m_Value.toString();
		Object valueEvaluated = m_helperEvaluation.evaluateObject(dataSelection, this);
		String scAfter = valueEvaluated.toString();
		System.out.println(String.format("Get %s value [%s] now [%s]", dataSelection, scBefore, scAfter));
		if (scBefore.equals(scAfter)) return m_Value;
		
		// Value has changed then need to set flag for new value and check for dependencies
		super.updateValue(this); // Set flag for change
		WhorlEngine.whorlValue(dataSelection, valueEvaluated);
		
		return valueEvaluated;
	}
	
	@Override
	public ValueData updateValue(ValueData dataValue) 
	{
		// If the value math value then replace math value - do this before check for text since math is a subclass of text 
		if (dataValue instanceof ValueDataMath) return super.updateValue(dataValue);		
		// If the value text value then try and change it to a number and set value at the end of a chain of math nodes
		if (dataValue instanceof ValueDataText) return isNumberUpdate(dataValue);		
		// Replace existing with new value and set flag indicating new value
		return super.updateValue(dataValue);
	}

	private ValueData isNumberUpdate(ValueData dataValue) 
	{
		// ValueData is text so get the value will not trigger any evaluation
		Object valueText = dataValue.getValue(null);
		String scValue = valueText.toString();
		
		try
		{
			Integer.parseInt(scValue);
			// If _not_ an int then throws exception bypasses this bit
			return updateNumber(dataValue);
		}
		catch (NumberFormatException x)
		{
			// Not an integer
		}
		try
		{
			Double.parseDouble(scValue);
			return updateNumber(dataValue);
		}
		catch (NumberFormatException x)
		{
			// Not a double
		}
		
		// Value cannot be used for a math node but set new value flag
		return super.updateValue(this);
	}

	public ValueData updateNumber(ValueData valueSource)
	{
		ValueData valueParent = this;
		ValueData valueUpdate = this;
		ValueData valuePrevious = this;
		
		while (null!=valuePrevious)
		{
			valueParent = valueUpdate;
			valueUpdate = valuePrevious;
			valuePrevious = valueUpdate.getNextValue();
		}
		
		if (valueUpdate instanceof ValueDataMath)
		{
			// Tack new value to the end of the list
			ValueDataMath valueMath = (ValueDataMath) valueUpdate;
			Object value = valueSource.getValue(null);
			valueMath.setExpression(value.toString());
			return super.updateValue(this);
		}
		
		if (valueUpdate instanceof ValueDataText)
		{
			// Replace final text with new text value
			valueParent.setNextValue(valueSource);
			return super.updateValue(this);
		}
		
		// Do not change the topmost value but set new value flag
		return super.updateValue(this);
	}

	// Get value for just expression
	public Object resetExpression(SelectionData dataSelection, String scExpression)
	{
		// Recalc the value attached to selection but make sure value reset so change is properly detected
		m_Value = "";
		Object valueExpression = m_helperEvaluation.evaluateObject(dataSelection, scExpression);
		setExpression(valueExpression.toString());
		
		return valueExpression;
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleElement(sm_scIdentifier, m_scText);
	}
	
	@Override
	public ValueData mergeValue()
	{
		// Need to reset the value so first access triggers new value
		m_Value = ""; // "" is not a valid math value!
		return this;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("= ");
		sb.append(m_Value.toString());
		sb.append(" = ");
		return sb.toString();
	}
}

class SelectionDataEvent
{
	SelectionDataEvent(NotifyData dataNotify, boolean zSelected)
	{
		m_dataNotify = dataNotify;
		m_zSelected = zSelected;
	}
	
	private NotifyData m_dataNotify = null;
	private boolean m_zSelected;
	
	public boolean isSelect() 
	{
		return m_zSelected;
	}
	
	public boolean isCascade() 
	{
		return false;
	}
	
	public boolean fireCharacterEvent(CharacterEvent eventSelect)
	{
		return m_dataNotify.fireCharacterEvent(eventSelect);
	}

	public boolean fireErrorEvent(String scError)
	{
		return m_dataNotify.fireCharacterEvent(new CharacterEventError(scError));
	}

	protected NotifyData getSink() 
	{
		return m_dataNotify;
	}
}

class SelectionDataAction implements Serializable, CharacterHelper
{
	private static final long serialVersionUID = -6150008809936587991L;

	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		selectionSource.setSelected(eventSelection.isSelect());
		return true; // Always want to return true otherwise raise an error
	}
	
	public SelectionDataAction next()
	{
		return null;
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		// Write the action tag
		return true;
	}

	@Override
	public boolean persistContent(PersistHelper helperPersist) 
	{
		// Write the action content
		return true;
	}

	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		// Set action content
		return helperParse.reportError(String.format("Untreated <%s> (%s) selection member", scType, scContent));
	}

	@Override
	public boolean handleView(ViewHelper helperView, JComponent componentView) 
	{
		// Actions probably don't have a view
		return true;
	}

	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		// Merge actions into selection - no need to merge the root
		return true;
	}
	
	@Override
	public String toString()
	{
		return "select/deselect";
	}
}

class SelectionDataActionNext extends SelectionDataAction
{
	private static final long serialVersionUID = 8603874199297950686L;
	private SelectionDataAction m_actionNext;
	
	public boolean setNextAction(SelectionData dataSelection)
	{
		m_actionNext = dataSelection.getAction();
		return dataSelection.setAction(this);
	}
	
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		if (selectActionNext(selectionSource, eventSelection)) return m_actionNext.selectAction(selectionSource, eventSelection);
		return false;
	}
	
	@Override
	public SelectionDataAction next()
	{
		return m_actionNext;
	}
	
	public boolean selectActionNext(SelectionData selectionSource, SelectionDataEvent eventSelect)
	{
		return true;
	}
	
	@Override
	public String toString()
	{
		return "next";
	}
}

class SelectionDataActionTarget extends SelectionDataActionNext
{
	private static final long serialVersionUID = -7960523077023776809L;

	SelectionDataActionTarget()
	{
		// Need empty constructor for restore
	}
	
	SelectionDataActionTarget(SelectionData selectionTarget)
	{
		m_selectionTarget = selectionTarget;			
	}
	
	protected SelectionData m_selectionTarget = null;
	// Need category for intermediate store when restoring
	private CategoryData m_dataCategory = null;
	
	protected boolean persistData(PersistHelper helperPersist, String scIdentifier) 
	{
		return helperPersist.handleSelectionList(m_selectionTarget, scIdentifier);
	}
	
	protected boolean restoreContent(ParseHelper helperParse, String scIdentifier, boolean zStart, boolean zEnd, String scType, String scContent)
	{
		if (zStart) return true;
		// When receive next tag can release back from local
		if (zEnd) return (scIdentifier!=scType);
		
		// Sets members for class
		if (restoreSelection(helperParse, scType, scContent)) return true;
		
		// Process the content as a continuation of other data
		return helperParse.reportError(String.format("Unexpected <%s> (%s) while restoring %s", scType, scContent, scIdentifier));
	}
	
	protected boolean restoreSelection(ParseHelper helperParse, String scType, String scContent)
	{
		if (CategoryData.sm_scIdentifier==scType)
		{
			m_dataCategory = helperParse.requireExistingCategory(scContent);
			return true;
		}
		
		if (SelectionData.sm_scIdentifier==scType)
		{
			m_selectionTarget = helperParse.requireExistingSelection(m_dataCategory, scContent);
			return true;
		}
		
		return false;
	}
	
	public SelectionData requireExistingSelection(MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		// Context of action to merge from and the selection to merge to
		String scCategory = (m_selectionTarget.getParent()).getName();
		String scSelection = m_selectionTarget.getName();
		return helperMerge.requireExistingSelection(dataCharacterMerge, scCategory, scSelection);
	}
	
	public String toString(String scVerb)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(scVerb);
		CategoryData dataCategory = m_selectionTarget.getParent();
		sb.append(dataCategory.getName());
		sb.append('.');
		sb.append(m_selectionTarget.getName());
		return sb.toString();
	}
}

class SelectionDataAction_suggestSelection extends SelectionDataActionTarget
{
	private static final long serialVersionUID = 187757057409170416L;
	public final static String sm_scIdentifier = "SUGGESTS_SELECTION";

	public static boolean createAction(SelectionData selectionSource, SelectionData selectionTarget)
	{
		SelectionDataAction_suggestSelection action = new SelectionDataAction_suggestSelection(selectionTarget);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_suggestSelection()
	{
		// Need version without arguments for restore
	}
	
	private SelectionDataAction_suggestSelection(SelectionData selectionTarget)
	{
		super(selectionTarget);
	}
	
	@Override
	public boolean selectActionNext(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		return m_selectionTarget.setSuggestion(eventSelection, selectionSource);
	}		
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return super.persistData(helperPersist, sm_scIdentifier);
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		return super.restoreContent(helperParse, sm_scIdentifier, zStart, zEnd, scType, scContent);
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		// Context of action to merge from and the selection to merge to
		SelectionData selectionTarget = requireExistingSelection(helperMerge, dataCharacterMerge, dataRegionMerge);
		return createAction(selectionSource, selectionTarget);
	}
	
	@Override
	public String toString()
	{
		return toString("suggests ");
	}
}

class SelectionDataAction_suggestCategory extends SelectionDataActionNext
{
	private static final long serialVersionUID = -6696829678012634307L;
	public final static String sm_scIdentifier = "SUGGESTS_CATEGORY";

	public static boolean createAction(SelectionData selectionSource, CategoryData categoryTarget)
	{
		SelectionDataAction_suggestCategory action = new SelectionDataAction_suggestCategory(categoryTarget);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_suggestCategory()
	{
		// Need version without arguments for restore
	}
	
	private SelectionDataAction_suggestCategory(CategoryData dataCategory)
	{
		m_dataCategory = dataCategory;
	}
	
	private CategoryData m_dataCategory = null;
	
	@Override
	public boolean selectActionNext(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		return m_dataCategory.setSuggestion(selectionSource, eventSelection);
	}		
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleCategoryList(m_dataCategory, sm_scIdentifier);
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		if (zStart) return true;
		// When receive next remove can release back from local
		if (zEnd) return (sm_scIdentifier!=scType);
		
		if (CategoryData.sm_scIdentifier==scType)
		{
			m_dataCategory = helperParse.requireExistingCategory(scContent);
			return true;
		}
		
		// Process the content as a continuation of other data
		return helperParse.reportError(String.format("Unexpected <%s> (%s) while restoring suggest category", scType, scContent));
	}

	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		// Context of action to merge from and the selection to merge to
		CategoryData dataCategory = helperMerge.requireExistingCategory(dataCharacterMerge, m_dataCategory.getName());
		return createAction(selectionSource, dataCategory);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("suggests all ");
		sb.append(m_dataCategory.getName());
		return sb.toString();
	}
}

class SelectionDataAction_exclude extends SelectionDataActionTarget
{
	private static final long serialVersionUID = 4999295449324723233L;
	public static final String sm_scIdentifier = "EXCLUDES_SELECTION";

	public static boolean createAction(SelectionData selectionSource, SelectionData selectionTarget)
	{
		SelectionDataAction_exclude action = new SelectionDataAction_exclude(selectionTarget);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_exclude()
	{
		// Need version constructor without arguments for restoring
	}
	
	private SelectionDataAction_exclude(SelectionData dataSelection)
	{
		super(dataSelection);
	}
	
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		// Run through the rest of the actions to exclude all in the particular categories
		if (!super.selectAction(selectionSource, eventSelection)) return false;
		// ... but afterwards re-enable the selection not suppressed
		eventSelection = new SelectionDataEvent(selectionSource, false);  // always set false so that when selection is removed clears not sets the exclusion
		return m_selectionTarget.setExclusion(eventSelection, selectionSource);
	}
	
	@Override
	public boolean selectActionNext(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		CategoryData dataCategory = m_selectionTarget.getParent();
		return dataCategory.setExclusion(selectionSource, eventSelection);
	}		
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return super.persistData(helperPersist, sm_scIdentifier);
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		return super.restoreContent(helperParse, sm_scIdentifier, zStart, zEnd, scType, scContent);
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		
		// Context of action to merge from and the selection to merge to
		String scCategory = (m_selectionTarget.getParent()).getName();
		String scSelection = m_selectionTarget.getName();
		SelectionData dataSelection = helperMerge.requireExistingSelection(dataCharacterMerge, scCategory, scSelection);

		return createAction(selectionSource, dataSelection);
	}
	
	@Override
	public String toString()
	{
		return toString("excludes all except");
	}
}

class SelectionDataAction_popup extends SelectionDataActionTarget
{
	private static final long serialVersionUID = -4960735622800578788L;
	public final static String sm_scIdentifier = "POPUP";
	
	public static boolean createAction(SelectionData selectionSource, RegionData regionTarget)
	{
		SelectionDataAction_popup action = new SelectionDataAction_popup(selectionSource, regionTarget);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_popup()
	{
		// Need version constructor without arguments for restoring
	}
	
	private SelectionDataAction_popup(SelectionData selectionSource, RegionData dataRegion)
	{
		super(selectionSource);
		m_dataRegion = dataRegion;
	}
	
	private RegionData m_dataRegion = null;
	
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		// Run through the rest of the actions so select state is set
		if (!super.selectAction(selectionSource, eventSelection)) return false;
		// ... but afterwards popup any windows selected
		if (selectionSource.isSelected()) return eventSelection.fireCharacterEvent(new CharacterEventPopup(m_dataRegion));
		return true;
	}
	
	public boolean hasRegion(String scRegion)
	{
		if (scRegion.equals(m_dataRegion.getName())) return true;
		if (m_dataRegion.hasRegion(scRegion)) return true;
		return false;
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleSelectionElement(sm_scIdentifier, m_dataRegion);
	}		
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		// When receive next popup can release back from local
		if (sm_scIdentifier==scType)
		{
			// First region parsed is the popup
			m_dataRegion = helperParse.dataParseLocal(0);
			// The parent for the popup is the selection
			m_selectionTarget.addRegion(m_selectionTarget, m_dataRegion);
			return helperParse.endParseLocal();
		}
		
		// Continue to process the content as a continuation of other data - not an error
		return false;
	}

	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		
		// Context of action to merge from and the selection to merge to
		String scRegion = m_dataRegion.getName();
		RegionData dataRegion = selectionSource.getRegion(scRegion);
		if (null==dataRegion) throw new RuntimeException("Could not get region " + scRegion);
		
		return SelectionDataAction_popup.createAction(selectionSource, dataRegion);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("popup ");
		sb.append(m_dataRegion.getName());
		return sb.toString();
	}
}

class SelectionDataAction_popback extends SelectionDataActionTarget
{
	private static final long serialVersionUID = 7513173183414881759L;
	public final static String sm_scIdentifier = "POPBACK";

	public static boolean createAction(SelectionData selectionSource, SelectionData selectionTarget)
	{
		SelectionDataAction_popback action = new SelectionDataAction_popback(selectionTarget);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_popback()
	{
		// Need version without arguments for restore
	}
	
	private SelectionDataAction_popback(SelectionData selectionTarget)
	{
		super(selectionTarget);
	}
	
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		// Run through the rest of the actions so select state is set
		if (!super.selectAction(selectionSource, eventSelection)) return false;
		
		if (selectionSource.isSelected()) return addSelection(selectionSource, eventSelection);

		return removeSelection(selectionSource, eventSelection);
	}
	
	private boolean addSelection(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(m_selectionTarget.getName());
		sb.append(" - ");
		sb.append(selectionSource.getName());
		
		CategoryData dataCategory = m_selectionTarget.getParent();
		SelectionData dataSelection = new SelectionData(sb.toString());
		dataSelection.removableSelection(selectionSource);

		// Preselect the added selection
		//dataSelection.setSelected(dataNotify, true);
		
		NotifyData notifySink = eventSelection.getSink();
		return dataCategory.addRegionWithView(notifySink, null, dataSelection);
		
	}
	
	private boolean removeSelection(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(m_selectionTarget.getName());
		sb.append(" - ");
		sb.append(selectionSource.getName());
		
		CategoryData dataCategory = m_selectionTarget.getParent();
		if (!dataCategory.hasRegion(sb.toString())) return true;
		SelectionData dataSelection = (SelectionData)dataCategory.getRegion(sb.toString());
		
		NotifyData notifySink = eventSelection.getSink();
		return dataCategory.removeRegion(notifySink, dataSelection);
	}
			
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return super.persistData(helperPersist, sm_scIdentifier);
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		return super.restoreContent(helperParse, sm_scIdentifier, zStart, zEnd, scType, scContent);
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		
		// Context of action to merge from and the selection to merge to
		String scCategory = (m_selectionTarget.getParent()).getName();
		String scSelection = m_selectionTarget.getName();
		SelectionData dataSelection = helperMerge.requireExistingSelection(dataCharacterMerge, scCategory, scSelection);
		
		return SelectionDataAction_popback.createAction(selectionSource, dataSelection);
	}
	
	@Override
	public String toString()
	{
		return toString("popback to");
	}
}

class SelectionDataAction_remove extends SelectionDataActionTarget
{
	private static final long serialVersionUID = 2816083688638583316L;

	public static boolean createAction(SelectionData selectionSource, SelectionData selectionTarget)
	{
		SelectionDataAction_remove action = new SelectionDataAction_remove(selectionTarget);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_remove()
	{
		// Need version without arguments for restore		
	}
	
	private SelectionDataAction_remove(SelectionData selectionTarget)
	{
		super(selectionTarget);
	}
	
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		// Run through the rest of the actions so select state is set
		if (!super.selectAction(selectionSource, eventSelection)) return false;
		if (!eventSelection.isSelect()) return removeSelection(selectionSource, eventSelection);
		return true;
	}
	
	private boolean removeSelection(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		NotifyData notifySink = eventSelection.getSink();	
		// Set source of the selection as this - event won't get anywhere
		m_selectionTarget.setSelected(notifySink, false);
		CategoryData dataCategory = m_selectionTarget.getParent();
		if (!dataCategory.hasRegion(selectionSource.getName())) return true;
		dataCategory.removeRegion(dataCategory, selectionSource);
		return true;
	}
			
	// DO NOT persist or restore removable selection

	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		
		// Context of action to merge from and the selection to merge to
		String scCategory = (m_selectionTarget.getParent()).getName();
		String scSelection = m_selectionTarget.getName();
		SelectionData dataSelection = helperMerge.requireExistingSelection(dataCharacterMerge, scCategory, scSelection);
		return SelectionDataAction_remove.createAction(selectionSource, dataSelection);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("remove ");
		CategoryData dataCategory = (CategoryData)m_selectionTarget.getParent();
		sb.append(dataCategory.getName());
		sb.append('.');
		sb.append(m_selectionTarget.getName());
		return sb.toString();
	}
}

class SelectionDataAction_fixed extends SelectionDataActionNext
{
	private static final long serialVersionUID = 2056455196208089242L;
	public final static String sm_scIdentifier = "FIXED";

	public static boolean createAction(SelectionData selectionSource, boolean zFixed)
	{
		SelectionDataAction_fixed action = new SelectionDataAction_fixed(zFixed);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_fixed()
	{
	}
	
	private SelectionDataAction_fixed(boolean zFixed)
	{
		m_zFixed = zFixed;
	}
	
	private boolean m_zFixed;
					
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		// Run through the rest of the actions so select state is set
		if (!super.selectAction(selectionSource, eventSelection)) return false;
		return true;
	}
			
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleElement(sm_scIdentifier, Boolean.toString(m_zFixed));
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		if (zEnd) return false;
		m_zFixed = Boolean.parseBoolean(scContent);
		return true;
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;		
		// Context of action to merge from and the selection to merge to
		return SelectionDataAction_fixed.createAction(selectionSource, m_zFixed);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("fixed ");
		sb.append(m_zFixed?"selected":"deslected");
		return sb.toString();
	}
}

class SelectionDataAction_unique extends SelectionDataActionNext
{
	private static final long serialVersionUID = -2220972876809167548L;
	public final static String sm_scIdentifier = "UNIQUE";

	public static boolean createAction(SelectionData selectionSource, SelectionData selectionTarget)
	{
		SelectionDataAction_unique action = new SelectionDataAction_unique();
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_unique()
	{
	}
	
	private boolean m_zActionInProgress = false;
	
	@Override
	public boolean selectActionNext(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		if (!eventSelection.isSelect()) return true;
		CategoryData dataCategory = selectionSource.getParent();

		if (m_zActionInProgress) eventSelection.fireErrorEvent(String.format("Unique selection %s.%s loop detected", dataCategory.getName(), selectionSource.getName()));
		if (m_zActionInProgress) return true;
		
		NotifyData notifySink = eventSelection.getSink();
		
		m_zActionInProgress = true;
		dataCategory.setUnique(notifySink, selectionSource);
		m_zActionInProgress = false;
		return true;
	}		
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return super.persistData(helperPersist);
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		return super.restoreContent(helperParse, sm_scIdentifier, zStart, zEnd, scType);
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		// Context of action to merge from and the selection to merge to
		return SelectionDataAction_unique.createAction(selectionSource, selectionSource);
	}
	
	@Override
	public String toString()
	{
		return "unique";
	}
}

class SelectionDataAction_replace extends SelectionDataActionNext
{
	private static final long serialVersionUID = -14006769961454290L;
	public final static String sm_scIdentifier = "REPLACE_REGION";
	public final static String sm_scCategory = "REPLACE_CATEGORY";

	public static boolean createAction(SelectionData selectionSource, RegionData dataRegion, CategoryData dataCategoryPopup)
	{
		SelectionDataAction_replace action = new SelectionDataAction_replace(dataRegion, dataCategoryPopup);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_replace()
	{
		// Default to create during restore
	}
	
	private SelectionDataAction_replace(RegionData dataRegion, CategoryData dataCategoryPopup)
	{
		m_dataRegion = dataRegion;
		m_dataCategoryChild = dataCategoryPopup;
	}
	
	private RegionData m_dataRegion = null;
	private CategoryData m_dataCategoryChild = null;
	
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		// Run through the rest of the actions so select state is set
		if (!super.selectAction(selectionSource, eventSelection)) return false;
		// ... but afterwards replace region
		if (!selectionSource.isSelected()) return true;
		
		NotifyData notifySink = eventSelection.getSink();
		
		// Removes the _view_ - category is not removed from character and content remains
		if (!m_dataRegion.detachRegions(notifySink)) return eventSelection.fireErrorEvent(String.format("Could not remove all from region %s", m_dataRegion.getName()));
		
		if (null==m_dataCategoryChild) throw new IllegalArgumentException("Selection data does not have category child set");
		
		// Then add to region
		if (!m_dataRegion.addRegion(notifySink, m_dataCategoryChild)) return eventSelection.fireErrorEvent(String.format("Could not add %s to region %s", m_dataCategoryChild.getName(), m_dataRegion.getName()));
		
		return true;
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		if (!helperPersist.handleSelectionElement(sm_scIdentifier, m_dataRegion)) return false;
		if (!helperPersist.handleElement(sm_scCategory, m_dataCategoryChild.getName())) return false;
		return true;
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		// When receive identifier release back from local but continue for category
		if (sm_scIdentifier==scType)
		{
			if (zStart) return true;
			if (zEnd) return true;			
			
			m_dataRegion = helperParse.dataParseLocal(0);
			// the next local content is for the category so return true
			return true;
		}
		
		// When receive category continue using this data
		if (sm_scCategory==scType)
		{
			if (zStart) return true;
			if (zEnd) return false;			
			
			m_dataCategoryChild = helperParse.requireExistingCategory(scContent);
			m_dataCategoryChild = helperParse.referenceCategory(scContent);
			// can now end local content
			return true;
		}
		
		// Content of tag includes region so process until end tag
		return false;
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		
		// Context of action to merge from and the selection to merge to
		TreeNode[] adataRegion = m_dataRegion.getPath();
		
		StringBuffer sb = new StringBuffer();
		String scD = "";
		int iPathLast = adataRegion.length - 1;
		
		for (int iIndex = 1; iIndex<iPathLast; ++iIndex)
		{
			sb.append(scD);
			RegionData dataRegion = (RegionData)adataRegion[iIndex];
			sb.append(dataRegion.getName());
			scD = ":";
		}
		
		RegionData dataRegion = (RegionData)adataRegion[iPathLast];
		dataRegion = helperMerge.requireExistingRegion(sb.toString(), dataRegion.getName());
		// Make sure category exists
		CategoryData dataCategoryReplace = helperMerge.requireExistingCategory(dataCharacterMerge, m_dataCategoryChild.getName());
		// Then get a proxy for the category
		dataCategoryReplace = dataCharacterMerge.referenceCategory(m_dataCategoryChild.getName());

		return SelectionDataAction_replace.createAction(selectionSource, dataRegion, dataCategoryReplace);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("put '");
		sb.append(m_dataCategoryChild.getName());
		sb.append("' in '");			
		sb.append(m_dataRegion.getName());
		sb.append("' replacing content");			
		return sb.toString();
	}
}

class SelectionDataAction_substitute extends SelectionDataActionNext
{
	private static final long serialVersionUID = -1408860910198299467L;
	public final static String sm_scIdentifier = "SUBST_TARGET";
	public final static String sm_scSource = "SUBST_FROM";

	public static boolean createAction(SelectionData selectionSource, RegionData dataTarget, RegionData dataFrom)
	{
		SelectionDataAction_substitute action = new SelectionDataAction_substitute(dataTarget, dataFrom);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_substitute()
	{
		// Default to create during restore
	}
	
	private SelectionDataAction_substitute(RegionData dataTarget, RegionData dataFrom)
	{
		m_dataTarget = dataTarget;
		m_dataFrom = dataFrom;
	}
	
	private RegionData m_dataTarget = null;
	private RegionData m_dataFrom = null;
	
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		// Run through the rest of the actions so select state is set
		if (!super.selectAction(selectionSource, eventSelection)) return false;
		// ... but afterwards replace region
		if (!selectionSource.isSelected()) return true;
		
		NotifyData notifySink = eventSelection.getSink();
		
		// Removes the _view_ - category is not removed from character and content remains
		if (!m_dataTarget.detachRegions(notifySink)) return eventSelection.fireErrorEvent(String.format("Could not remove all from region %s", m_dataTarget.getName()));
		
		// Remove the target from the current parent
		RegionData dataParent = (RegionData)m_dataFrom.getParent();
		
		if (null!=dataParent)
		{
			if (!dataParent.removeRegion(notifySink, m_dataFrom)) return eventSelection.fireErrorEvent(String.format("Could not detach %s from parent %s", m_dataFrom.getName(), dataParent.getName()));
		}
		
		// Then add to region
		if (!m_dataTarget.addRegion(notifySink, m_dataFrom)) return eventSelection.fireErrorEvent(String.format("Could not add %s to region %s", m_dataFrom.getName(), m_dataTarget.getName()));
		
		return true;
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		if (!helperPersist.handleSelectionElement(sm_scIdentifier, m_dataTarget)) return false;
		if (!helperPersist.handleRegion(m_dataFrom.getName(), m_dataFrom, sm_scSource)) return false;
		return true;
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		// When receive identifier release back from local but continue for category
		if (sm_scIdentifier==scType)
		{
			if (zStart) return true;
			if (zEnd) return true;			
			
			m_dataTarget = helperParse.dataParseLocal(0);
			// the next local content is for the region so return true
			return true;
		}
		
		// When receive category continue using this data
		if (sm_scSource==scType)
		{
			if (zStart) return true;
			if (zEnd) return false;			
			
			m_dataFrom = helperParse.dataParseLocal(0);
			// the next local content is for the region so return true
			return true;
		}
		
		// Content of tag includes region so process until end tag
		return false;
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		
		// Context of action to merge from and the selection to merge to
		TreeNode[] adataRegion = m_dataTarget.getPath();
		
		StringBuffer sb = new StringBuffer();
		String scD = "";
		int iPathLast = adataRegion.length - 1;
		
		for (int iIndex = 1; iIndex<iPathLast; ++iIndex)
		{
			sb.append(scD);
			RegionData dataRegion = (RegionData)adataRegion[iIndex];
			sb.append(dataRegion.getName());
			scD = ":";
		}
		
		// Get reference for the target data region to move
		RegionData dataTarget = (RegionData)adataRegion[iPathLast];
		dataTarget = helperMerge.requireExistingRegion(sb.toString(), dataTarget.getName());
		if (null==dataTarget) throw new IllegalArgumentException("Selection data does not have target region data set");
		
		// Get a proxy for the view where region is attached
		RegionData dataRoot = dataCharacterMerge.getRoot();
		RegionData dataSource = dataRoot.getRegion(m_dataFrom.getName());
		if (null==dataSource) throw new IllegalArgumentException("Selection data does not have source region data set");

		// Create new action
		return SelectionDataAction_substitute.createAction(selectionSource, dataTarget, dataSource);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("put '");
		sb.append(m_dataFrom.getName());
		sb.append("' in '");			
		sb.append(m_dataTarget.getName());
		sb.append("' substituting content");			
		return sb.toString();
	}
}

class SelectionDataAction_math extends SelectionDataActionTarget
{
	private static final long serialVersionUID = 6141055713836505118L;
	public final static String sm_scIdentifier = "MATH_SELECTION";
	public final static String sm_scExpression = "MATH_VALUE";

	public static boolean createAction(SelectionData selectionSource, SelectionData selectionTarget, EvaluationHelper helperEvaluation, String scExpression)
	{
		SelectionDataAction_math action = new SelectionDataAction_math(selectionTarget, helperEvaluation, scExpression);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_math()
	{
		// Need version without arguments for restore
	}
	
	private SelectionDataAction_math(SelectionData selectionTarget, EvaluationHelper helperEvaluation, String scExpression)
	{
		super(selectionTarget);
		m_dataValueMath = new ValueDataMath(helperEvaluation, scExpression);
		m_scExpression = scExpression;
	}
	
	private ValueDataMath m_dataValueMath = null;
	private String m_scExpression = null;
	
	@Override
	public boolean selectActionNext(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		if (eventSelection.isSelect()==selectionSource.isSelected())
		{
			// Already attached value
			return eventSelection.fireCharacterEvent(new CharacterEventSelect(EventSelectEnum._CHANGE_SELECTED, m_selectionTarget));
		}
					
		// Attach or detach value
		ValueData valueAttach = m_dataValueMath.getNextValue();
		ValueData valueNext = m_selectionTarget.getData();
		
		if (eventSelection.isSelect())
		{
			// Selected the data to attach is the data from the action
			
			// If selected the tail data for the data from the action is the current data
			// ... default above
			// If selected the data to attach is the data from the action
			valueAttach = m_dataValueMath;
			
			// (Re)set the data value so next access to selection sees new value
			m_dataValueMath.resetExpression(m_selectionTarget, m_scExpression);
		}
		else
		{
			// If unselected the tail data for the data from the action is terminated
			valueNext = null;
			// If unselected the data to attach is the tail of the data from the action
			// ... default above

			// (Re)set the data value so next access to selection sees new value
			if (valueAttach instanceof ValueDataMath)
			{
				ValueDataMath valueMath = (ValueDataMath) valueAttach;
				String scExpression = valueMath.getExpression();
				valueMath.resetExpression(m_selectionTarget, scExpression);
			}
		}
		
		if (valueNext==valueAttach)
		{
			return eventSelection.fireErrorEvent(String.format("Error changing %s value from expression [%s]", m_selectionTarget.toString(), m_scExpression));
		}

		// The data selection set fires the change value event {if the value has really changed}
		m_dataValueMath.setNextValue(valueNext);
		m_selectionTarget.setData(valueAttach);
		
		return true;
	}
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		if (!helperPersist.handleSelectionList(m_selectionTarget, sm_scIdentifier)) return false;
		if (!helperPersist.handleElement(sm_scExpression, m_scExpression)) return false;
		return true;
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		if (zStart) return true;
		
		boolean zReturn = true;
		
		if (sm_scIdentifier==scType)
		{
			// Still have the expression to come
			zReturn = true;
		}
		
		if (sm_scExpression==scType)
		{
			m_dataValueMath = new ValueDataMath(EvaluationHelper.currentEvaluationHelper(), m_scExpression);
			// When no content for expression can return from local
			zReturn = false;
		}
		
		// Only want to set members when there is content
		if (zEnd) return zReturn;
		
		// Sets members for super class
		if (restoreSelection(helperParse, scType, scContent)) return true;
		
		if (sm_scExpression==scType)
		{
			m_scExpression = scContent;
			// Drop the local parse context
			return true;
		}
		
		// Process the content as a continuation of other data
		return helperParse.reportError(String.format("Unexpected <%s> (%s) while restoring selection math", scType, scContent));
	}

	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		
		// Context of action to merge from and the selection to merge to
		String scCategory = (m_selectionTarget.getParent()).getName();
		String scSelection = m_selectionTarget.getName();
		SelectionData selectionTarget = helperMerge.requireExistingSelection(dataCharacterMerge, scCategory, scSelection);
		
		// Create the action in the context of the selection that it is attached to
		return SelectionDataAction_math.createAction(selectionSource, selectionTarget, dataCharacterMerge.getEvaluation(), m_scExpression);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(" value '");
		sb.append(m_dataValueMath.toString());
		sb.append("' for '");
		sb.append(m_selectionTarget.getName());
		sb.append("' from '");			
		sb.append(m_scExpression);			
		sb.append("' ");			
		return sb.toString();
	}
}	

class SelectionDataAction_cascade extends SelectionDataActionNext
{
	private static final long serialVersionUID = -6990743815540095284L;
	public final static String sm_scIdentifier = "CASCADE_SELECTION";

	public static boolean createAction(SelectionData selectionSource, SelectionData selectionTarget, boolean zState)
	{
		SelectionDataAction_cascade action = new SelectionDataAction_cascade(selectionTarget, zState);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_cascade()
	{
		// Need to have constructor without arguments for restoring state
	}
	
	private SelectionDataAction_cascade(SelectionData dataSelection, boolean zState)
	{
		m_dataSelection = dataSelection;
		m_zState = zState;
	}
	
	private CategoryData m_dataCategory = null;
	private SelectionData m_dataSelection = null;
	private boolean m_zState;
	private transient boolean m_zActionInProgress = false;
	
	@Override
	public boolean selectActionNext(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		CategoryData dataCategory = m_dataSelection.getParent();

		if (m_zActionInProgress) eventSelection.fireCharacterEvent(new CharacterEventError(String.format("Cascaded selection %s.%s loop detected", dataCategory.getName(), selectionSource.getName())));
		if (m_zActionInProgress) return true;
		
		NotifyData notifySink = eventSelection.getSink();
		SelectionDataEvent eventSelectionCascade = new SelectionDataEvent(notifySink, m_zState)
		{
			@Override
			public boolean isCascade() {return true;}
		};
		
		m_zActionInProgress = true;
		m_dataSelection.setSelected(eventSelectionCascade);
		m_zActionInProgress = false;

		return true;
	}		
	
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleSelectionList(m_dataSelection, sm_scIdentifier);
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		if (zStart) return true;
		// When receive next cascade can release back from local
		if (zEnd) return (sm_scIdentifier!=scType);
		
		if (CategoryData.sm_scIdentifier==scType)
		{
			m_dataCategory = helperParse.requireExistingCategory(scContent);
			return true;
		}
		
		if (SelectionData.sm_scIdentifier==scType)
		{
			m_dataSelection = helperParse.requireExistingSelection(m_dataCategory, scContent);
			return true;
		}
		
		// Process the content as a continuation of other data
		return helperParse.reportError(String.format("Unexpected <%s> (%s) while restoring cascade", scType, scContent));
	}

	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		
		// Context of action to merge from and the selection to merge to
		String scCategory = (m_dataSelection.getParent()).getName();
		String scSelection = m_dataSelection.getName();
		SelectionData selectionTarget = helperMerge.requireExistingSelection(dataCharacterMerge, scCategory, scSelection);
	
		return SelectionDataAction_cascade.createAction(selectionSource, selectionTarget, m_zState);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("cascades ");
		CategoryData dataCategory = m_dataSelection.getParent();
		sb.append(dataCategory.getName());
		sb.append('.');
		sb.append(m_dataSelection.getName());
		return sb.toString();
	}
}

class SelectionDataAction_toggle extends SelectionDataActionNext
{
	private static final long serialVersionUID = -1909249761184601251L;
	public final static String sm_scIdentifier = "TOGGLE";

	public static boolean createAction(SelectionData selectionSource, boolean zToggle)
	{
		SelectionDataAction_toggle action = new SelectionDataAction_toggle(zToggle);
		return action.setNextAction(selectionSource);
	}
	
	SelectionDataAction_toggle()
	{
	}
	
	private SelectionDataAction_toggle(boolean zToggle)
	{
		m_zToggle = zToggle;
	}
	
	private boolean m_zToggle;
					
	@Override
	public boolean selectAction(SelectionData selectionSource, SelectionDataEvent eventSelection)
	{
		NotifyData notifySink = eventSelection.getSink();		
		SelectionDataEvent eventSent = new SelectionDataEvent(notifySink, m_zToggle);

		// Normally send the toggle event unless the event is a cascade
		if (eventSelection.isCascade()) eventSent = eventSelection;
		
		// For toggle only generate the transition to toggle state
		if (!super.selectAction(selectionSource, eventSent)) return false;
		
		return true;
	}
			
	@Override
	public boolean persistData(PersistHelper helperPersist) 
	{
		return helperPersist.handleElement(sm_scIdentifier, Boolean.toString(m_zToggle));
	}
	
	@Override
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent)
	{
		if (zStart) return true;
		if (zEnd) return false;
		m_zToggle = Boolean.parseBoolean(scContent);
		return true;
	}
	
	@Override
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge) 
	{
		SelectionData selectionSource = (SelectionData)dataRegionParent;
		// Context of action to merge from and the selection to merge to
		return SelectionDataAction_toggle.createAction(selectionSource, m_zToggle);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("toggle ");
		sb.append(m_zToggle?"selected":"deslected");
		return sb.toString();
	}
}

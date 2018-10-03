package generator;

import java.awt.Dimension;
import java.io.File;

public class CharacterEvent
{
	CharacterEvent()
	{
	}
	
	CharacterEvent(RegionData dataRegion)
	{
		m_dataRegion = dataRegion;	
	}

	public enum EventViewEnum {_SHOW, _RESIZE, _ADD_FILE, _POPUP, _LAYOUT}
	public enum EventEditEnum {_UPDATE_ALL, _REMOVE_ALL, _ADD_REGION, _REMOVE_REGION, _ADD_CATEGORY, _REMOVE_CATEGORY, _ADD_SELECTION, _REMOVE_SELECTION}
	public enum EventSelectEnum {_CHANGE_SELECTED, _SET_SUGGESTION, _SET_EXCLUSION, _CHANGE_VALUE}	

	private RegionData m_dataRegion = null;

	public RegionData getSource()
	{
		return m_dataRegion;
	}
	
	@Override
	public String toString()
	{
		return m_dataRegion.toString();
	}

	// This gets override for each type of character listener for select, edit and error
	public boolean listenerAction(CharacterListener listener) 
	{
		return false;
	}
}

class CharacterEventView extends CharacterEvent
{
	CharacterEventView(EventViewEnum enumEdit)
	{
		m_enumEdit = enumEdit;
	}
	
	CharacterEventView(EventViewEnum enumEdit, RegionData dataRegion)
	{
		super(dataRegion);
		m_enumEdit = enumEdit;
	}
	
	private EventViewEnum m_enumEdit = EventViewEnum._SHOW;

	public EventViewEnum getType()
	{
		return m_enumEdit;
	}
	
	@Override
	public boolean listenerAction(CharacterListener listener) 
	{
		return listener.changeView(this);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(m_enumEdit.toString());
		sb.append(' ');
		sb.append(super.toString());
		return sb.toString();
	}	
}

class CharacterEventPopup extends CharacterEventView
{
	CharacterEventPopup(RegionData dataRegion)
	{
		super(EventViewEnum._POPUP, dataRegion);
	}	
}

class CharacterEventShow extends CharacterEventView
{
	CharacterEventShow(CharacterData dataCharacter, RegionData dataRegion)
	{
		super(EventViewEnum._SHOW, dataRegion);
		m_dataCharacter = dataCharacter;
		
	}
	
	CharacterEventShow(CharacterData dataCharacter, RegionData dataRegion, EventViewEnum enumView)
	{
		super(enumView, dataRegion);
		m_dataCharacter = dataCharacter;
	}
	
	private CharacterData m_dataCharacter = null;
	
	public CharacterData getCharacter()
	{
		return m_dataCharacter;
	}	
}

class CharacterEventEdit extends CharacterEvent
{
	CharacterEventEdit(EventEditEnum enumEdit)
	{
		m_enumEdit = enumEdit;
	}
	
	CharacterEventEdit(EventEditEnum enumEdit, RegionData dataRegion)
	{
		super(dataRegion);
		m_enumEdit = enumEdit;
	}
	
	private EventEditEnum m_enumEdit = EventEditEnum._UPDATE_ALL;
	
	public EventEditEnum getType()
	{
		return m_enumEdit;
	}
	
	@Override
	public boolean listenerAction(CharacterListener listener) 
	{
		return listener.madeEdit(this);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(m_enumEdit.toString());
		sb.append(' ');
		sb.append(super.toString());
		return sb.toString();
	}
}

class CharacterEventLoaded extends CharacterEventEdit
{
	// File loaded event extends edit event to update all
	CharacterEventLoaded(File fileLoaded)
	{
		super(EventEditEnum._UPDATE_ALL);
		m_fileLoaded = fileLoaded;
	}
	
	private File m_fileLoaded = null;
	
	public File getFileLoaded()
	{
		return m_fileLoaded;
	}	
}

class CharacterEventSelect extends CharacterEvent
{
	CharacterEventSelect(EventSelectEnum enumSelection, RegionData dataRegion)
	{
		super(dataRegion);
		m_enumSelection = enumSelection;
	}
	
	CharacterEventSelect(EventSelectEnum enumSelection, RegionData dataRegion, boolean zSelected)
	{
		super(dataRegion);
		m_enumSelection = enumSelection;
		m_zSelected = zSelected;
	}
	
	// Event is also used by set suggestion and set exclusion by using different types
	private EventSelectEnum m_enumSelection = EventSelectEnum._CHANGE_SELECTED;
	private boolean m_zSelected = false;
	
	public EventSelectEnum getType()
	{
		return m_enumSelection;
	}
	
	public boolean isSelected()
	{
		return m_zSelected;
	}
	
	@Override
	public boolean listenerAction(CharacterListener listener) 
	{
		return listener.madeSelection(this);
	}
}

class CharacterEventValue extends CharacterEventSelect
{
	CharacterEventValue(EventSelectEnum enumSelection, RegionData dataRegion)
	{
		super(enumSelection, dataRegion);
	}
	
	CharacterEventValue(EventSelectEnum enumSelection, RegionData dataRegion, ValueData dataValue)
	{
		super(enumSelection, dataRegion);
		m_ValueData = dataValue;
	}
	
	private ValueData m_ValueData = null;
	
	public ValueData getValue()
	{
		return m_ValueData;
	}
}

class CharacterEventError extends CharacterEvent
{
	CharacterEventError(String scError)
	{
		m_scError = scError;
	}
	
	private String m_scError = null;
	
	public String getError()
	{
		return m_scError;
	}	

	@Override
	public boolean listenerAction(CharacterListener listener) 
	{
		return listener.failMessage(this);
	}
}

class CharacterEventResize extends CharacterEventView
{
	CharacterEventResize(Dimension dimension)
	{
		super(EventViewEnum._RESIZE);
		m_dimension = dimension;
	}
	
	private Dimension m_dimension = null;
	
	public Dimension getObject()
	{
		return m_dimension;
	}
}

class CharacterEventRecurse extends CharacterEventShow
{
	CharacterEventRecurse(CharacterData dataCharacter, String scFileNameRecurse)
	{
		super(dataCharacter, null, EventViewEnum._ADD_FILE);
		m_scFileNameRecurse = scFileNameRecurse;
	}
	
	private String m_scFileNameRecurse = null;

	public String getFile()
	{
		return m_scFileNameRecurse;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(EventViewEnum._ADD_FILE.toString());
		sb.append(" ");
		sb.append(m_scFileNameRecurse);
		sb.append(" ");
		return sb.toString();
	}
}

class CharacterEventLayout extends CharacterEventShow
{
	CharacterEventLayout(CharacterData dataCharacter, RegionData dataRegion, boolean zShow)
	{
		super(dataCharacter, dataRegion, EventViewEnum._LAYOUT);
		m_zShow = zShow;
	}
	
	private boolean m_zShow = false;
	
	public boolean isShowLayout()
	{
		return m_zShow;
	}
}

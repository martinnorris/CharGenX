package generator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;

class LockableList extends ArrayList<CharacterListener>
{
	private static final long serialVersionUID = -5357125830289902317L;

	private boolean m_zLocked = false;
	
	public synchronized void lock()
	{
		if (m_zLocked) 
			throw new RuntimeException("List locked");
		m_zLocked = true;
	}
	
	public synchronized void unlock()
	{
		m_zLocked = false;
	}
}

class LockableQueue extends ArrayList<CharacterEvent>
{
	private static final long serialVersionUID = -8433571927757411153L;

	private boolean m_zProcessing = false;

	public synchronized boolean queueEventButDelayProcess(CharacterEvent eventEdit) 
	{
		add(eventEdit);
		if (m_zProcessing) 
			return true;
		m_zProcessing = true;
		return false;
	}

	public synchronized boolean hasEvent() 
	{
		if (1>size()) return false;
		return true;
	}

	public synchronized CharacterEvent processEvent() 
	{
		int iLast = size()-1;
		return remove(iLast);
	}

	public synchronized void endProcess() 
	{
		m_zProcessing = false;
	}
}

class NotifyData extends DefaultMutableTreeNode implements Serializable
{
	private static final long serialVersionUID = -2649452633860743994L;
	
	public NotifyData()
	{
		m_listListener = new LockableList();
		m_listRemove = new LockableList();
		m_queueEvent = new LockableQueue();
	}

	transient private LockableList m_listListener = null;
	transient private LockableList m_listRemove = null;
	transient private LockableQueue m_queueEvent = null;
	
	transient private boolean m_zSuppressEvent = false;
	
	private void readObject(ObjectInputStream streamIn) throws IOException, ClassNotFoundException
	{
		streamIn.defaultReadObject();
		m_listListener = new LockableList();
		m_listRemove = new LockableList();
		m_queueEvent = new LockableQueue();
		return;
	}
	
	public NotifyData getNotify()
	{
		NotifyData dataParent = (NotifyData)getParent();
		if (null==dataParent) return this;
		return dataParent.getNotify();
	}

	private boolean addNodeListenerRoot(CharacterListener listener)
	{
		try
		{
			m_listListener.lock();
			if (m_listListener.contains(listener)) return true;
			m_listListener.add(listener);
		}
		finally
		{
			m_listListener.unlock();
		}
		
		return true;
	}
	
	public boolean addNodeListener(CharacterListener listener)
	{
		NotifyData dataRoot = getNotify();
		return dataRoot.addNodeListenerRoot(listener);		
	}
	
	private boolean mergeNodeListenerRoot(NotifyData dataMerge)
	{
		LockableList listListener = dataMerge.m_listListener;
		
		try
		{
			listListener.lock();
			for (CharacterListener listener : listListener)
				addNodeListener(listener);
		}
		finally
		{
			listListener.unlock();
		}
		
		return true;		
	}
	
	public boolean mergeNodeListener(NotifyData dataMerge)
	{
		NotifyData dataRoot = getNotify();
		return dataRoot.mergeNodeListenerRoot(dataMerge);
	}
	
	private boolean removeNodeListenerRoot(CharacterListener listener)
	{
		try
		{
			m_listRemove.lock();
			if (m_listRemove.contains(listener)) return true;
			m_listRemove.add(listener);
		}
		finally
		{
			m_listRemove.unlock();
		}
		
		return true;
	}
	
	private boolean removeNodeListenerExecute()
	{
		try
		{
			m_listListener.lock();
			m_listRemove.lock();
			
			for (CharacterListener listener : m_listRemove)
				if (m_listListener.contains(listener)) m_listListener.remove(listener);
			
			m_listRemove.clear();
		}
		finally
		{
			m_listListener.unlock();
			m_listRemove.unlock();
		}
		
		return true;
	}
	
	public boolean removeNodeListener(CharacterListener listener)
	{
		NotifyData dataRoot = getNotify();
		return dataRoot.removeNodeListenerRoot(listener);		
	}
	
	protected boolean removeThis(RegionData dataParent)
	{
		m_listListener.clear();
		return true;
	}

	public boolean suppressEvent(boolean zSuppressEvent)
	{
		m_zSuppressEvent = zSuppressEvent;
		return m_zSuppressEvent;
	}
	
	public boolean fireCharacterEvent(CharacterEvent eventCharacter)
	{
		if (m_zSuppressEvent) return true;
		NotifyData dataRoot = getNotify();
		return dataRoot.fireCharacterEventQueued(eventCharacter);
	}	
	
	private boolean fireCharacterEventQueued(CharacterEvent eventQueue)
	{
		// Queue and get processing lock
		if (m_queueEvent.queueEventButDelayProcess(eventQueue)) return true;
			
		try
		{
			while (m_queueEvent.hasEvent())
			{
				CharacterEvent eventProcess = m_queueEvent.processEvent();
				fireCharacterEventRoot(eventProcess);
			}
		}
		finally
		{
			m_queueEvent.endProcess();			
		}
		
		return true;
	}
	
	private boolean fireCharacterEventRoot(CharacterEvent eventCharacter)
	{
		try
		{
			m_listListener.lock();
			
			// Generate event for category/selection
			for (CharacterListener listener : m_listListener)
				if (!eventCharacter.listenerAction(listener)) return false;
		}
		finally
		{
			m_listListener.unlock();
		}
		
		return removeNodeListenerExecute();		
	}	
}

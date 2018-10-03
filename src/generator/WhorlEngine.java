package generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

/*
 * Have a terrible circular dependency A = C+1, B = A+1, C = B+1
 * Need to getValue for A - evaluates C - evaluates B - _gets_ A value
 * B = 1 queues Whorl B
 * C = 2 queues Whorl C
 * A = 3 queues Whorl A
 * 
 * B processed dependency C - evaluates B - evaluates A - _gets_ C value
 * A = 3 queues Whorl A
 * B = 4 queues Whorl B
 * C = 5 queues Whorl C
 * 
 * 
 * 
 * 
 */

/*

class LockableListSelectionData extends ArrayList<SelectionData>
{
	private static final long serialVersionUID = 5250755246400253804L;

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

 */

class WhorlEngine extends Thread
{
	private static WhorlDispatch m_whorlDispatch = null;

	public static WhorlEngine getWhorlEngine(CharacterData dataCharacter)
	{
		if (null==m_whorlDispatch)
		{
			m_whorlDispatch = new WhorlDispatch();
			m_whorlDispatch.start();
		}
		
		WhorlEngine engineReturn = new WhorlEngine();
		engineReturn.startWhorlEngine(dataCharacter);
		return engineReturn;
	}
	
	public static boolean whorlValue(SelectionData dataSelection, Object valueSelection)
	{
		// Map the value so do not call selection which causes an evaluation
		// Use dispatch to separate the whorl value call from the work
		return m_whorlDispatch.queueDispatch(dataSelection, valueSelection);
	}
	
	public boolean startWhorlEngine(CharacterData dataCharacter)
	{
		m_queueWhorlEngine = new LinkedList<SelectionData>();
		m_mapWhorlDependents = new HashMap<SelectionData, WhorlDependency>();
		m_whorlEvaluation = new WhorlEvaluation(dataCharacter);
		start();
		return true;
	}
	
	private Queue<SelectionData> m_queueWhorlEngine = null;
	private Map<SelectionData, WhorlDependency> m_mapWhorlDependents = null;
	private WhorlEvaluation m_whorlEvaluation = null;
	
	public boolean queueWhorl(SelectionData dataSelection)
	{
		synchronized (m_queueWhorlEngine)
		{
			m_queueWhorlEngine.add(dataSelection);
			m_queueWhorlEngine.notify();
		}
		return true;
	}
	
	@Override
	public void run() 
	{
		while (whorlEngine())
		{
			// Try and recover?
			System.out.println("Problem processing whorl value but trying again");
		}
		return;
	}
	
	private boolean whorlEngine()
	{
		while (true)
		{
			try 
			{
				SelectionData dataSelection = null;
				synchronized (m_queueWhorlEngine)
				{
					while(m_queueWhorlEngine.isEmpty()) m_queueWhorlEngine.wait();
					dataSelection = m_queueWhorlEngine.remove();
				}
				changeValue(dataSelection);
			} 
			catch (InterruptedException x) 
			{
				x.printStackTrace();
				break;
			}
			catch (NoSuchElementException x) 
			{
				x.printStackTrace();
				break;				
			}
		}
		
		// When return true then tries to recover from exception
		return true;				
	}
	
	private WhorlDependency getWhorlDependency(SelectionData dataSelection)
	{
		WhorlDependency dependsReturn = m_mapWhorlDependents.get(dataSelection);
		
		if (null==dependsReturn)
		{
			dependsReturn = new WhorlDependency();
			m_mapWhorlDependents.put(dataSelection, dependsReturn);
		}
		
		return dependsReturn;
	}

	private boolean changeValue(SelectionData dataSelection)
	{
		// Get the selection dependency
		WhorlDependency dependsThis = getWhorlDependency(dataSelection);
		System.out.println("Change for " + dataSelection);
		
		// Check current value of the selection against the stored value
		Object valueSelection = m_whorlDispatch.getSelectionValue(dataSelection);
		if (dependsThis.sameValueAndReplace(valueSelection)) return true;

		// Check and build list of dependencies
		if (!buildSelectionDependency(dependsThis, dataSelection)) return true;

		// Value has changed
		
		// Evaluate all the selections that have this selection
		Iterator<SelectionData> iterateDepends = dependsThis.getOtherUseThis();
		
		while (iterateDepends.hasNext())
		{
			// This will trigger the selection depending on this to evaluate
			SelectionData dataOther = iterateDepends.next();
			System.out.println("Check for " + dataOther);
			Object valueDepends = dataOther.getValue();
			System.out.println(String.format("Dependent value for %s [%s]", dataOther, valueDepends.toString()));
		}
		
		return true;
	}
	
	private boolean buildSelectionDependency(WhorlDependency dependsThis, SelectionData dataThis) 
	{
		// Evaluate the selection math to extract the dependencies
		
		ValueDataMath valueMath = (ValueDataMath)dataThis.getData();
		m_whorlEvaluation.evaluateObject(dataThis, valueMath);
		Set<SelectionData> setThisUsesOther = m_whorlEvaluation.getThisUsesOther();

		// Need to check for existing dependencies and those removed
		List<SelectionData> listRemoveDependency = new ArrayList<SelectionData>();
		
		// Check existing list against the new list
		Iterator<SelectionData> iterateExisting = dependsThis.getThisUsesOther();
		while (iterateExisting.hasNext())
		{
			SelectionData dataOther = iterateExisting.next();
			if (setThisUsesOther.contains(dataOther))
			{
				// Remove a match in the new list and the current so not added afterwards
				setThisUsesOther.remove(dataOther);
				continue;				
			}
			// Create a list of dependencies so do not disturb underlying arrays
			listRemoveDependency.add(dataOther);
		}
		
		Iterator<SelectionData> iterateRemove = listRemoveDependency.iterator();
		while (iterateExisting.hasNext())
		{
			SelectionData dataOther = iterateRemove.next();
			// Remove this from other and vice versa
			if (removeOtherUsesThis(dataOther, dataThis)) dependsThis.removeThisUsesOther(dataOther);			
		}
		
		Iterator<SelectionData> iterateAdditional = setThisUsesOther.iterator();
		while (iterateAdditional.hasNext())
		{
			SelectionData dataOther = iterateAdditional.next();
			// Add new dependency
			if (addOtherUsesThis(dataOther, dataThis)) dependsThis.addThisUsesOther(dataOther);
		}
		
		return dependsThis.hasDependencies();
	}

	private boolean addOtherUsesThis(SelectionData dataOther, SelectionData dataThis) 
	{
		// Add this to the other dependencies
		WhorlDependency dependsOther = getWhorlDependency(dataOther);
		return dependsOther.addOtherUsesThis(dataThis);
	}

	private boolean removeOtherUsesThis(SelectionData dataOther, SelectionData dataThis) 
	{
		// Remove this from other dependencies
		WhorlDependency dependsOther = getWhorlDependency(dataOther);
		return dependsOther.removeOtherUsesThis(dataThis);
	}
}

class WhorlEvaluation extends EvaluationHelper
{
	public WhorlEvaluation(CharacterData dataCharacter)
	{
		super(dataCharacter);
		m_dataCharacter = dataCharacter;
		m_Reference = new WhorlReference();
		m_Result = new ParseResultInteger(0);
		m_setThisUsesOther = new HashSet<SelectionData>();
	}
	
	private CharacterData m_dataCharacter = null;
	private ParseReference m_Reference = null;
	private ParseResultInteger m_Result = null;
	private Set<SelectionData> m_setThisUsesOther = null;
	
	// Override parse result generated so does not try and get selection value
	class WhorlReference extends ParseReference
	{
		@Override
		public ParseResultInteger getResult()
		{
			return m_Result;
		}		
	}
	
	@Override
	protected ParseReference referenceSymbol(ParseArgument argument) throws EvaluationHelperException
	{
		m_Reference.reset();
		
		while (argument.hasSymbol())
		{
			String scSource = argument.getStringRegex();
			if (null==scSource) break;
			m_Reference.element(m_dataCharacter, scSource);
			
			int iCharacter =  argument.nextChar();			
			if ('.'!=iCharacter) break;
			argument.consume(1);			
		}
		
		m_setThisUsesOther.add(m_Reference.getReference());
		
		return m_Reference;
	}
	
	public Set<SelectionData> getThisUsesOther()
	{
		Set<SelectionData> listReturn = m_setThisUsesOther;
		m_setThisUsesOther = new HashSet<SelectionData>();
		return listReturn;
	}
}

class WhorlDispatch extends Thread
{
	public WhorlDispatch()
	{
		m_queueDispatchEngine = new LinkedList<SelectionData>();
		m_mapSelectionValue = new HashMap<SelectionData, Object>();
	}
	
	private Queue<SelectionData> m_queueDispatchEngine = null;
	private Map<SelectionData, Object> m_mapSelectionValue = null;
	
	public boolean queueDispatch(SelectionData dataSelection, Object valueSelection)
	{
		m_mapSelectionValue.put(dataSelection, valueSelection);

		System.out.println("Queue for " + dataSelection);
		
		synchronized (m_queueDispatchEngine)
		{
			m_queueDispatchEngine.add(dataSelection);
			m_queueDispatchEngine.notify();
		}
		
		return true;
	}
	
	public Object getSelectionValue(SelectionData dataSelection) 
	{
		return m_mapSelectionValue.get(dataSelection);
	}

	@Override
	public void run() 
	{
		while (whorlDispatch())
		{
			// Try and recover?
			System.out.println("Problem processing whorl dispatch but trying again");
		}
		return;
	}
	
	private boolean whorlDispatch()
	{
		while (true)
		{
			try 
			{
				SelectionData dataSelection = null;
				synchronized (m_queueDispatchEngine)
				{
					while (m_queueDispatchEngine.isEmpty()) m_queueDispatchEngine.wait();
					dataSelection = m_queueDispatchEngine.remove();
				}
				WhorlEngine whorlEngine = dataSelection.getWhorlEngine();
				whorlEngine.queueWhorl(dataSelection);
			} 
			catch (InterruptedException x) 
			{
				x.printStackTrace();
				break;
			}
			catch (NoSuchElementException x) 
			{
				x.printStackTrace();
				break;				
			}
			catch (Exception x)
			{
				x.printStackTrace();
				break;								
			}
		}
		
		// When return true then tries to recover from exception
		return true;				
	}

}

class WhorlDependency
{
	private Object m_valueSelection = null;
	private List<SelectionData> m_listThisUsesOther = null;
	private List<SelectionData> m_listOtherUsesThis = null;
	
	public WhorlDependency()
	{
		m_valueSelection = this;
		m_listThisUsesOther = new ArrayList<SelectionData>();
		m_listOtherUsesThis = new ArrayList<SelectionData>();
	}
	
	public boolean sameValueAndReplace(Object valueSelection) 
	{
		// Need to perform a string comparison
		String scStored = m_valueSelection.toString();
		String scCurrent = valueSelection.toString();
		
		// Replace stored value
		m_valueSelection = valueSelection;
		
		return scStored.equals(scCurrent);
	}

	public boolean hasDependencies() 
	{
		return 0<m_listOtherUsesThis.size();
	}

	public boolean addThisUsesOther(SelectionData dataSelection) 
	{
		return m_listThisUsesOther.add(dataSelection);
	}

	public boolean removeThisUsesOther(SelectionData dataSelection) 
	{
		return m_listThisUsesOther.remove(dataSelection);
	}

	public boolean addOtherUsesThis(SelectionData dataSelection) 
	{
		return m_listOtherUsesThis.add(dataSelection);
	}

	public boolean removeOtherUsesThis(SelectionData dataSelection) 
	{
		return m_listOtherUsesThis.remove(dataSelection);
	}

	public Iterator<SelectionData> getThisUsesOther() 
	{
		return m_listThisUsesOther.iterator();
	}

	public Iterator<SelectionData> getOtherUseThis() 
	{
		return m_listOtherUsesThis.iterator();
	}
}

package generator;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingWorker;

class Controls implements ActionListener
{
	Controls(CharacterData dataChar, CharacterListener listenerData)
	{
		m_dataChar = dataChar;
		m_listenerData = listenerData;
	}

	private CharacterData m_dataChar = null;
	private CharacterListener m_listenerData = null;
	
	@Override
	public void actionPerformed(ActionEvent eventAction) 
	{
		Component componentParent = null;
		Object oSource = eventAction.getSource();
		if (oSource instanceof Component) componentParent = (Component)oSource;
		
		interpretAction(componentParent, eventAction.getActionCommand());
		
		return;
	}
	
	public boolean interpretAction(Component componentParent, String scAction)
	{
		if (!interpretActionThrowError(componentParent, scAction))
		{
			RegionData dataRoot = m_dataChar.getRoot();
			dataRoot.fireCharacterEvent(new CharacterEventError("Could not " + scAction));
			return false;
		}
		return true;
	}
	
	// Execute in a different thread
	@SuppressWarnings("unused")
	private boolean interpretActionWorker(final Component componentParent, final String scAction, long lTimeout) 
	{
		SwingWorker<Boolean, Void> workerAction = new SwingWorker<Boolean, Void>()
				{
					@Override
					protected Boolean doInBackground() throws Exception 
					{
						return new Boolean(interpretActionThrowError(componentParent, scAction));
					}
				};
				
		Boolean booleanResult;
		
		try 
		{
			booleanResult = workerAction.get(lTimeout, null);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
			return false;
		} 
		catch (ExecutionException e) 
		{
			e.printStackTrace();
			return false;
		} 
		catch (TimeoutException e) 
		{
			e.printStackTrace();
			return false;
		}
		
		return booleanResult.booleanValue();
	}
		
	@SuppressWarnings("unused")
	private boolean interpretActionCatch(Component componentParent, String scAction) 
	{
		try
		{
			if (!interpretActionThrowError(componentParent, scAction)) return false;
		}
		catch (ClassCastException x)
		{
			ActionPerform.showTree(m_dataChar, x.getMessage());
		}
		catch (NullPointerException x)
		{
			ActionPerform.showTree(m_dataChar, x.getMessage());
		}
		return true;
	}
	
	private boolean interpretActionThrowError(Component componentParent, String scAction) 
	{
		if (scAction.startsWith("Select"))
		{
			ActionPerformSelect actionSelect = new ActionPerformSelect();
			if (!actionSelect.interpretAction(m_dataChar, scAction)) return m_listenerData.failMessage(new CharacterEventError("Use 'Select <Category> <Selection> <true|false>"));
			if (!actionSelect.actionPerform(componentParent, m_dataChar)) m_listenerData.failMessage(new CharacterEventError(String.format("Could not %s", scAction)));
			return true;
		}
		
		if (scAction.startsWith("Value"))
		{
			ActionPerformValue actionValue = new ActionPerformValue();
			if (!actionValue.interpretAction(m_dataChar, scAction)) return m_listenerData.failMessage(new CharacterEventError("Use 'Value <Category> <Selection> <value>"));
			if (!actionValue.actionPerform(componentParent, m_dataChar)) m_listenerData.failMessage(new CharacterEventError(String.format("Could not %s", scAction)));
			return true;
		}
		
		if (scAction.startsWith("Expression"))
		{
			ActionPerformExpression actionExpression = new ActionPerformExpression();
			if (!actionExpression.interpretAction(m_dataChar, scAction)) return m_listenerData.failMessage(new CharacterEventError("Use 'Expression <Category> <Selection> <expression>"));
			if (!actionExpression.actionPerform(componentParent, m_dataChar)) m_listenerData.failMessage(new CharacterEventError(String.format("Could not %s", scAction)));
			return true;
		}
		
		if (scAction.startsWith("Exit"))
		{
			System.exit(m_dataChar.exit());
		}
		
		if (scAction.startsWith("New"))
		{
			ActionPerformNew actionValue = new ActionPerformNew();
			if (!actionValue.actionPerform(componentParent, m_dataChar)) return m_listenerData.failMessage(new CharacterEventError("Could not reset current data"));
			return m_listenerData.madeEdit(new CharacterEventLoaded(null));
		}
		
		if (scAction.startsWith("Blank"))
		{
			return m_dataChar.resetSelection();
		}
		
		if (scAction.startsWith("Merge"))
		{
			ActionPerformMerge actionMerge = new ActionPerformMerge(m_listenerData);
			if (!actionMerge.interpretAction(m_dataChar, scAction)) return m_listenerData.failMessage(new CharacterEventError("Use 'Merge [<name of file>]'"));
			if (!actionMerge.actionPerform(componentParent, m_dataChar)) return m_listenerData.failMessage(new CharacterEventError("Could not merge file"));

			// TODO move event generation lower level - though that means passing action to lower levels too
			
			// Fire event to inform existing character data of change
			CharacterEventLoaded eventMerged = new CharacterEventLoaded(actionMerge.getLoadedFile());
			RegionData dataRootExisting = m_dataChar.getRoot();
			dataRootExisting.fireCharacterEvent(eventMerged);
			
			// Fire event to inform source that merge completed
			return m_listenerData.madeEdit(eventMerged);
		}
		
		if (scAction.startsWith("LoadXML"))
		{
			ActionPerformLoadXML actionLoad = new ActionPerformLoadXML(m_listenerData);
			if (!actionLoad.actionPerform(componentParent, m_dataChar)) return m_listenerData.failMessage(new CharacterEventError("Could not load file"));
			return m_listenerData.madeEdit(new CharacterEventLoaded(actionLoad.getLoadedFile()));
		}
		
		if (scAction.startsWith("LoadData"))
		{
			ActionPerformLoadData actionLoad = new ActionPerformLoadData();
			if (!actionLoad.actionPerform(componentParent, m_dataChar)) return m_listenerData.failMessage(new CharacterEventError("Could not load file"));
			return m_listenerData.madeEdit(new CharacterEventLoaded(actionLoad.getLoadedFile()));
		}
		
		if (scAction.startsWith("SaveXML"))
		{
			Actions actionSave = new ActionPerformSaveXML();
			if (!actionSave.actionPerform(componentParent, m_dataChar)) return m_listenerData.failMessage(new CharacterEventError("Could not save file"));
			return true;
		}
		
		if (scAction.startsWith("SaveData"))
		{
			Actions actionSave = new ActionPerformSaveData();
			if (!actionSave.actionPerform(componentParent, m_dataChar)) return m_listenerData.failMessage(new CharacterEventError("Could not save file"));
			return true;
		}
		
		if (scAction.startsWith("Show"))
		{
			ActionPerformShow actionShow = new ActionPerformShow();
			if (!actionShow.interpretAction(m_dataChar, scAction)) return false;
			if (!actionShow.actionPerform(componentParent, m_dataChar)) return false;
			return true;
		}
		
		if (scAction.startsWith("Tree"))
		{
			ActionPerform.showTree(m_dataChar, "Data Structure");
			return true;
		}
		
		if (scAction.startsWith("Layout"))
		{
			ActionPerformLayout actionLayout = new ActionPerformLayout();
			if (!actionLayout.interpretAction(m_dataChar, scAction)) return false;
			if (!actionLayout.actionPerform(componentParent, m_dataChar)) return false;
			return true;
		}
		
		if (scAction.startsWith("Resize"))
		{
			ActionPerformResize actionResize = new ActionPerformResize();
			if (!actionResize.interpretAction(m_dataChar, scAction)) return m_listenerData.failMessage(new CharacterEventError("Use 'Resize [x] [y]"));
			if (!actionResize.actionPerform(componentParent, m_dataChar)) m_listenerData.failMessage(new CharacterEventError("Could not set size"));
			return true;
		}
		
		if (scAction.startsWith("Help"))
		{
			if (componentParent!=null)
			{
				// Help popup can callback with other actions
				ActionHelp actionHelp = new ActionHelp(this);
				return actionHelp.actionPerform(componentParent, m_dataChar);
			}
			
			System.out.println("From command line/interactive shell:");
			System.out.println(" New - removes all categories and regions");
			System.out.println(" Blank - resets all categories");
			System.out.println(" LoadXML <file> - Loads XML file replacing all existing categories and regions");
			System.out.println(" Merge <file> - merges text file with existing categories and regions");
			System.out.println(" Show [category] [selection]");
			System.out.println(" Tree");
			System.out.println(" Select <category> <selection> <true|false>");
			System.out.println(" Value <category> <selection> <value>");
			return true;
		}
		
		if (scAction.startsWith("About"))
		{
			ActionAbout actionAbout = new ActionAbout();
			if (!actionAbout.actionPerform(componentParent, m_dataChar)) m_listenerData.failMessage(new CharacterEventError("Could not open 'About'"));
			return true;
		}
				
		return true;
	}
}
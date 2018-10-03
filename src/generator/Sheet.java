package generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import generator.CharacterEvent.EventEditEnum;
import generator.CharacterEvent.EventSelectEnum;
import generator.CharacterEvent.EventViewEnum;

public class Sheet
{
	private void writeLine(String scFormat)
	{
		System.out.println(scFormat);
		return;
	}
	
	private String readLine(final BufferedReader readerSystem, final String scPrompt, final int iSeconds)
	{
		Callable<String> callWithTimeout = new Callable<String>()
		{
			@Override
			public String call() throws IOException 
			{
				String scReturn;

				System.out.print(scPrompt);
				
				try 
				{
					while (!readerSystem.ready()) Thread.sleep(100);
					scReturn = readerSystem.readLine();
				} 
				catch (InterruptedException e) 
				{
					return null;
				}
				
				return scReturn;
			}			
		};
		
		ExecutorService threadExecute = Executors.newSingleThreadExecutor();
		Future<String> futureReturn = threadExecute.submit(callWithTimeout);
		
		String scReturn = "Q";
		
		try 
		{
			scReturn = futureReturn.get(iSeconds, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) 
		{
		} 
		catch (ExecutionException e) 
		{
		} 
		catch (TimeoutException e) 
		{
			scReturn = "W";
		}
		finally
		{
			threadExecute.shutdownNow();
		}
		
		return scReturn;
	}
	
	private boolean createConsole(Controls controlChar, String[] ascSetup)
	{
		if (0<ascSetup.length)
		{
			for (int iIndex = 0; iIndex<ascSetup.length; ++iIndex)
				if (controlChar.interpretAction(null, ascSetup[iIndex])) return true;
		}
		else
		{
			writeLine("Hit 'W' to display view, 'Q' to quit, or");
			controlChar.interpretAction(null, "Help");
		}
		
		if (createConsole(controlChar))
		{
			// Close console
			writeLine("... quit");
			return true;
		}
		
		// Go interactive
		writeLine("... interactive");
		return false;
	}
	
	private boolean createConsole(Controls controlChar)
	{
		// Console does not exist in some cases so use System.in
		BufferedReader readerSystem = new BufferedReader(new InputStreamReader(System.in));

		for (int iTimeout = 5;; iTimeout = 300)
		{
			String scAction = readLine(readerSystem, "> ", iTimeout);
			
			if (scAction.startsWith("W")) return false; // end console - open interactive
			if (scAction.startsWith("Q")) return true; // end console - end app

			if (!controlChar.interpretAction(null, scAction)) break;
		}
		
		return false;
	}
	
	private SheetListener getSheetListener(String scPrefix)
	{
		return new SheetListener(scPrefix);
	}
	
	private class SheetListener implements CharacterListener
	{
		private SheetListener(String scPrefix)
		{
			m_scPrefix = scPrefix;
			m_fileLoaded = null;
		}
		
		private String m_scPrefix;
		private File m_fileLoaded;
		
		public File getPathLoaded()
		{
			return m_fileLoaded;
		}
		
		@Override
		public boolean changeView(CharacterEventView eventView) 
		{
			EventViewEnum enumView = eventView.getType();
			
			switch (enumView)
			{
			case _ADD_FILE:
				CharacterEventRecurse eventRecurse = (CharacterEventRecurse)eventView;
				writeLine(String.format("%s %s ", m_scPrefix, eventView.toString()));
				return recurseEdit(eventRecurse);
				
			case _RESIZE:
				CharacterEventResize eventResize = (CharacterEventResize)eventView;
				writeLine(String.format("%s %s ", m_scPrefix, eventResize.toString()));
				return true;
				
			case _POPUP:
				writeLine(String.format("%s %s ", m_scPrefix, enumView.toString()));				
				return true;
				
			case _SHOW:
				CharacterEventShow eventShow = (CharacterEventShow)eventView;
				CharacterData dataCharacter = eventShow.getCharacter();				
				RegionData dataRegion = eventView.getSource();
				return showRegion(dataCharacter, dataRegion);
				
			case _LAYOUT:
				return true;
			}
			
			return false;
		}
		
		@Override
		public boolean madeSelection(CharacterEventSelect eventSelect) 
		{
			EventSelectEnum enumSelect = eventSelect.getType();
			SelectionData dataSelection = (SelectionData)eventSelect.getSource();
			CategoryData dataCategory = dataSelection.getParent();
			
			if (EventSelectEnum._CHANGE_SELECTED==eventSelect.getType())
			{
				writeLine(String.format("%s %s %s.%s %s ", m_scPrefix, enumSelect.toString(), dataCategory.getName(), dataSelection.getName(), Boolean.toString(eventSelect.isSelected())));
			}
			if (EventSelectEnum._CHANGE_VALUE==eventSelect.getType())
			{
				CharacterEventValue eventValue = (CharacterEventValue)eventSelect;
				writeLine(String.format("%s %s %s.%s %s ", m_scPrefix, enumSelect.toString(), dataCategory.getName(), dataSelection.getName(), eventValue.getValue().toString()));
			}
			return true;
		}

		@Override
		public boolean madeEdit(CharacterEventEdit eventEdit) 
		{
			RegionData dataRegion = eventEdit.getSource();
			EventEditEnum enumEdit = eventEdit.getType();
			
			if (eventEdit instanceof CharacterEventLoaded)
			{
				// Event type is _UPDATE_ALL after loading file
				CharacterEventLoaded eventLoaded = (CharacterEventLoaded)eventEdit;				
				m_fileLoaded = eventLoaded.getFileLoaded();
			}
			
			switch (enumEdit)
			{
			case _ADD_REGION:
			case _ADD_SELECTION:
			case _REMOVE_SELECTION:
				RegionData dataParent = (RegionData)dataRegion.getParent();
				writeLine(String.format("%s %s %s.%s ", m_scPrefix, enumEdit.toString(), dataParent.getName(), dataRegion.getName()));
				return true;
				
			case _ADD_CATEGORY:
			case _REMOVE_CATEGORY:
			case _REMOVE_REGION: // When removed node does not have parent
				writeLine(String.format("%s %s %s ", m_scPrefix, enumEdit.toString(), dataRegion.getName()));
				return true;
				
			case _UPDATE_ALL:
			case _REMOVE_ALL:
				writeLine(String.format("%s %s ", m_scPrefix, enumEdit.toString()));				
				return true;				
			}
			
			return false;
		}
		
		@Override
		public boolean failMessage(CharacterEventError eventError) 
		{
			writeLine(eventError.getError());
			return true;
		}
		
		private boolean showRegion(CharacterData dataCharacter, RegionData dataRegion)
		{
			if (dataRegion instanceof RegionDataRoot)
			{
				Iterator<CategoryData> iterateCategory = dataCharacter.getCategories();
				while (iterateCategory.hasNext())
				{
					RegionData category = iterateCategory.next();
					writeLine(String.format("%s ", category.toString()));					
				}
				return true;
			}
			
			if (dataRegion instanceof CategoryData)
			{
				CategoryData dataCategory = (CategoryData)dataRegion;
				
				for (@SuppressWarnings("unchecked")
				Enumeration<RegionData> enumerateSelection = dataCategory.children(); enumerateSelection.hasMoreElements();)
				{
					SelectionData dataSelection = (SelectionData) enumerateSelection.nextElement();
					writeLine(String.format("%s %s %s %s %s ", m_scPrefix, dataSelection.getName(), 
						dataSelection.isSuggested()?"(suggested)":"",
						dataSelection.isExcluded()?"(excluded)":"",
						dataSelection.isSelected()?"*SELECTED*":""));					
				}
				return true;
			}
			
			if (dataRegion instanceof SelectionData)
			{
				SelectionData dataSelection = (SelectionData)dataRegion;
				CategoryData dataCategory = (CategoryData)dataSelection.getParent();

				writeLine(String.format("%s %s.%s ", m_scPrefix, dataCategory.getName(), dataSelection.getName()));
				writeLine(String.format("\t %s %s %s", dataSelection.isSelected()?"*SELECTED*":"unset", dataSelection.isSuggested()?"(suggested)":"", dataSelection.isExcluded()?"(excluded)":""));

				for (Iterator<SelectionData> iterateExclusions = dataSelection.getExclusions(); iterateExclusions.hasNext();)
				{
					SelectionData dataSelectionExcluding = iterateExclusions.next();
					CategoryData dataCategoryExcluding = (CategoryData)dataSelectionExcluding.getParent();
					writeLine(String.format("\t excluded by %s.%s", dataCategoryExcluding.getName(), dataSelectionExcluding.getName()));											
				}
				
				for (SelectionDataAction actionSelect = dataSelection.getAction(); null!=actionSelect; actionSelect = actionSelect.next())
				{
					writeLine(String.format("\t action %s", actionSelect.toString()));					
				}
				
				writeLine(String.format("\t value %s", dataSelection.getValue().toString()));					
								
				return true;
			}
			
			return false;			
		}
		
		private boolean recurseEdit(CharacterEventRecurse eventRecurse) 
		{
			CharacterData dataChar = eventRecurse.getCharacter();
			String scFile = eventRecurse.getFile();
					
			ActionPerformMerge actionMerge = new ActionPerformMerge(this, scFile);
			if (!actionMerge.actionPerform(null, dataChar)) return failMessage(new CharacterEventError("Could not merge file"));
			
			return true;
		}

	}
	
	public static void main(String[] args) 
	{
		Sheet main = new Sheet();
		
		CharacterData dataChar = new CharacterData();
		CharacterListener listenerData = main.getSheetListener("CharacterData:");
		dataChar.addListener(listenerData);	
		
		SheetListener listenerControls = main.getSheetListener("Controls:");
		Controls controlChar = new Controls(dataChar, listenerControls);
		
		if (main.createConsole(controlChar, args)) return;
		
		File fileLoaded = listenerControls.getPathLoaded();
		CharacterView viewChar = new CharacterView(dataChar, controlChar, fileLoaded);
		dataChar.addListener(viewChar);
		
		viewChar.createPanel();
	}
}


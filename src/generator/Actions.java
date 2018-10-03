package generator;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.TreePath;

import generator.CharacterEvent.EventEditEnum;

interface Actions
{
	public boolean actionPerform(Component componentParent, CharacterData dataChar);	
}

class ActionPerform implements Actions
{
	ActionPerform()
	{
		
	}
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataChar)
	{
		return true;
	}
	
	public boolean interpretAction(CharacterData dataChar, String scParameters)
	{
		// Regex to separate "A B C" 'a b c' or A B C e.g. "A B C" 'a b c' A B C tokens 'A B C' 'a b c' 'A' 'B' 'C'
		String scRegex = "\"([^\"]*)\"|'([^']*)'|([^\"' ]+)";
		
		Pattern patternRegex = Pattern.compile(scRegex);
		Matcher matchRegex = patternRegex.matcher(scParameters);
		
		return setMembers(dataChar, matchRegex);	
	}
	
	protected boolean setMembers(CharacterData dataChar, Matcher matchRegex)
	{
		return true;
	}
	
	protected String removeQuotedGroup1(Matcher matchRegex)
	{
		String scReturn = null;
		for (int iGroup = 0, iCount = matchRegex.groupCount(); iGroup<iCount; ++iGroup)
		{
			scReturn = matchRegex.group(iGroup);
			if (null==scReturn) continue;
			if (scReturn.startsWith("\"")) continue;
			if (scReturn.startsWith("'")) continue;
			break;
		}
		return scReturn;
	}
	
	protected String removeQuotedGroup2(Matcher matchRegex)
	{
		String scReturn = matchRegex.group();
		if ('"'==scReturn.charAt(0)) return matchRegex.group(1);
		if ('\''==scReturn.charAt(0)) return matchRegex.group(2);
		return scReturn;
	}
	
	public static void showTree(CharacterData dataChar, String scTitle)
	{
		JTree treeData = new JTree(dataChar);
		
		ActionTreeSelection treeSelectionListener = new ActionTreeSelection();
		treeData.addTreeSelectionListener(treeSelectionListener);
		
		JScrollPane treeView = new JScrollPane(treeData);
		ViewHelper.frameView(treeView, null, scTitle, new Dimension(500, 400));
	}	
}

class ActionTreeSelection implements TreeSelectionListener
{
	@Override
	public void valueChanged(TreeSelectionEvent eventSelect) 
	{
		if (!eventSelect.isAddedPath()) return;
		TreePath pathSelected = eventSelect.getPath();
		popupObject(pathSelected.getLastPathComponent());
		return;
	}
	
	private boolean popupObject(Object oSelected)
	{
		if (oSelected instanceof SelectionData) return popupSelectionData((SelectionData)oSelected);
		return false;
	}
	
	private boolean popupSelectionData(SelectionData dataSelection)
	{
		JTextPane paneText = new JTextPane();
		paneText.setText(dataSelection.getState());
		
		JPanel panelSelectionData = new JPanel();
		panelSelectionData.add(paneText);
		
		ViewHelper.frameView(panelSelectionData, null, dataSelection.getName(), new Dimension(500, 400));
		return true;
	}
}

class ActionPerformResize extends ActionPerform
{
	private CharacterEventResize m_eventSend = null;
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataChar)
	{
		NotifyData dataRoot = dataChar.getRoot();
		return dataRoot.fireCharacterEvent(m_eventSend);
	}
	
	@Override
	public boolean interpretAction(CharacterData dataChar, String scParameters)
	{
		Scanner scanString = null;
		
		try
		{
			scanString = new Scanner(scParameters);
			return interpretActionWithClose(dataChar, scanString);
		}
		finally
		{
			scanString.close();
		}
	}
	
	private boolean interpretActionWithClose(CharacterData dataChar, Scanner scanString)
	{
		if (null==scanString.next()) return false;
		
		if (!scanString.hasNextInt()) return createDimension(400, 300);
		int iX = scanString.nextInt();
		if (!scanString.hasNextInt()) return createDimension(iX, 300);
		int iY = scanString.nextInt();
		
		return createDimension(iX, iY);		
	}
	
	private boolean createDimension(int iX, int iY)
	{
		m_eventSend = new CharacterEventResize(new Dimension(iX, iY));
		return true;
	}
}

class ActionPerformFile extends ActionPerform
{
	ActionPerformFile()
	{
	}
	
	private static JFileChooser m_chooseFile = null;
	protected File m_file = null;
	
	protected boolean setFileFilter(JFileChooser fileChooser)
	{
		FileFilter[] aFilters = fileChooser.getChoosableFileFilters();
		for (FileFilter filter : aFilters) fileChooser.removeChoosableFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(true);
		return true;
	}
	
	protected int showOpenDialog(Component componentParent)
	{
		if (null==m_chooseFile) setFileChooser();
		setFileFilter(m_chooseFile);
		return m_chooseFile.showOpenDialog(componentParent);
	}
	
	protected int showSaveDialog(Component componentParent)
	{
		if (null==m_chooseFile) setFileChooser();
		setFileFilter(m_chooseFile);
		return m_chooseFile.showSaveDialog(componentParent);
	}
	
	protected File getSelectedFile()
	{
		if (null==m_chooseFile) return null;
		return m_chooseFile.getSelectedFile();
	}
	
	protected File getFileFromChosen(String scFile)
	{
		File fileDirectory = m_chooseFile.getCurrentDirectory();
		Path pathDirectory = fileDirectory.toPath();
		Path pathFile = pathDirectory.resolve(scFile);
		return pathFile.toFile();
	}
	
	public File getLoadedFile()
	{
		return m_file;
	}
	
	private static void setFileChooser()
	{
		if (null!=m_chooseFile) return;
		
		File fileDirectory = chooseDirectory();

		m_chooseFile = new JFileChooser();
		m_chooseFile.setCurrentDirectory(fileDirectory);
		
		return;
	}
	
	private static File chooseDirectory()
	{
		String scHome = System.getProperty("user.home");
		
		StringBuilder sb = new StringBuilder();
		sb.append("Home (");
		sb.append(scHome);
		sb.append(")");
		
		class ActionListenerReturn implements ActionListener
		{
			private String m_scChosen = ".";
			
			@Override
			public void actionPerformed(ActionEvent eventButton) 
			{
				m_scChosen = eventButton.getActionCommand();
				JComponent optionPane = (JComponent)eventButton.getSource();
				Window windowFrame = (Window)SwingUtilities.getRoot(optionPane);
				windowFrame.dispatchEvent(new WindowEvent(windowFrame, WindowEvent.WINDOW_CLOSING));
				return;
			}
			
			private File fileChosen()
			{
				Path path = Paths.get(m_scChosen);
				return path.toFile();
			}
		}
		
		ActionListenerReturn listenButtons = new ActionListenerReturn();
		
		//URI uriResource = URI.create(".");
		URI uriRoot = URI.create("..");
		URI uriTests = URI.create(".");
		
		try 
		{
			URL urlResource = Sheet.class.getClassLoader().getResource("\\");
			URL urlRoot = new URL(urlResource, "..");
			URL urlTests = new URL(urlRoot, "Tests");
			
			//uriResource = urlResource.toURI();
			uriRoot = urlRoot.toURI();
			uriTests = urlTests.toURI();
		} 
		catch (URISyntaxException e) 
		{
		} 
		catch (MalformedURLException e) 
		{
		}
		
		String[] ascButtons = new String[]
				{
					sb.toString(), "Execution location", "Test examples"
				};
				
		Path[] aPaths = new Path[]
				{
					Paths.get(scHome),
					Paths.get(uriRoot),
					Paths.get(uriTests)
				};
			
		Object aoButtons[] = new Object[ascButtons.length];
			
		for (int iIndex = 0; iIndex<ascButtons.length; ++iIndex)
		{
			JButton button = new JButton(ascButtons[iIndex]);
			button.setAlignmentX(Component.CENTER_ALIGNMENT);
			button.addActionListener(listenButtons);
			button.setActionCommand(aPaths[iIndex].toString());
			aoButtons[iIndex] = button;
		}
		
		JOptionPane.showOptionDialog(null, "Choose folder:", "Set default file chooser folder", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, aoButtons, null);

		return listenButtons.fileChosen();
	}
}

class ActionPerformLoad extends ActionPerformFile
{	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataChar)
	{
		int iResponse = showOpenDialog(componentParent);
		if (JFileChooser.APPROVE_OPTION!=iResponse) return false;
		File file = getSelectedFile();
		return loadFileAndClose(dataChar, file);
	}

	boolean loadFileAndClose(CharacterData dataChar, File file)
	{
		try
		{
			if (!loadFile(dataChar, file)) return false;
			m_file = file;
		}
		catch (RuntimeException x)
		{
			System.err.println("Failed to interpret to file");
			
			NotifyData dataNotify = dataChar.getRoot();
			dataNotify.fireCharacterEvent(new CharacterEventError(String.format("Could not interpret '%s' because %s", file.getName(), x.getMessage())));

			return false;			
		}
		catch (IOException x)
		{
			System.err.println("Failed to load to file");
			
			NotifyData dataNotify = dataChar.getRoot();
			dataNotify.fireCharacterEvent(new CharacterEventError(String.format("Could not load '%s' because %s", file.getName(), x.getMessage())));

			return false;
		}
		
		return true;
	}

	protected boolean loadFile(CharacterData dataChar, File file) throws IOException
	{
		return true;
	}	
}

class ActionPerformLoadXML extends ActionPerformLoad
{
	ActionPerformLoadXML(CharacterListener listenerData)
	{
		m_listenerData = listenerData;
	}
	
	private CharacterListener m_listenerData = null;
	
	@Override
	protected boolean setFileFilter(JFileChooser fileChooser)
	{
		FileFilter[] aFilters = fileChooser.getChoosableFileFilters();
		for (FileFilter filter : aFilters) fileChooser.removeChoosableFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileFilter(new FileNameExtensionFilter("XML files", "xml"));
		return true;
	}
	
	@Override
	protected boolean loadFile(CharacterData dataChar, File file) throws IOException
	{
		PersistHelper dataCharHelper = new PersistHelper();
		InputStream streamIn = new FileInputStream(file);
		
		try 
		{
			CharacterData dataCharLoaded = dataCharHelper.loadDocument(streamIn, m_listenerData);
			if (!dataChar.removeAll()) return m_listenerData.failMessage(new CharacterEventError("Failed to load file "));
			dataChar.replaceData(dataCharLoaded, file);
		} 
		finally
		{
			streamIn.close();
		}
		
		return true;		
	}	
}

class ActionPerformLoadData extends ActionPerformLoad
{
	@Override
	protected boolean setFileFilter(JFileChooser fileChooser)
	{
		FileFilter[] aFilters = fileChooser.getChoosableFileFilters();
		for (FileFilter filter : aFilters) fileChooser.removeChoosableFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileFilter(new FileNameExtensionFilter("Data files", "bin"));
		return true;
	}
	
	@Override
	protected boolean loadFile(CharacterData dataChar, File file) throws IOException
	{
		ObjectInputStream inObject = new ObjectInputStream(new FileInputStream(file));
		
		try 
		{
			Object oLoaded = inObject.readObject();
			CharacterData dataCharLoaded = (CharacterData)oLoaded;
			dataChar.replaceData(dataCharLoaded, file);
		} 
		catch (ClassNotFoundException e) 
		{
			return false;
		}
		finally
		{
			inObject.close();
		}
		
		return true;		
	}
}


class ActionPerformSave extends ActionPerformFile
{	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataChar)
	{
		int iResponse = showSaveDialog(componentParent);
		if (JFileChooser.APPROVE_OPTION==iResponse) return saveFileAndClose(dataChar);
		return true;
	}
	
	private boolean saveFileAndClose(CharacterData dataChar)
	{
		try
		{
			File file = getSelectedFile();
			if (!saveFile(dataChar, file)) return false;
		}
		catch (IOException x)
		{
			System.err.println("Failed to save to file");
			return false;
		}
		
		return true;
	}	
	
	protected boolean saveFile(CharacterData dataChar, File file) throws IOException
	{
		return true;
	}
}

class ActionPerformSaveData extends ActionPerformSave
{
	@Override
	protected boolean setFileFilter(JFileChooser fileChooser)
	{
		FileFilter[] aFilters = fileChooser.getChoosableFileFilters();
		for (FileFilter filter : aFilters) fileChooser.removeChoosableFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileFilter(new FileNameExtensionFilter("Data files", "bin"));
		return true;
	}
	
	@Override
	protected boolean saveFile(CharacterData dataChar, File file) throws IOException
	{
		// TODO bug saves to parent folder
		String scName = file.getName().toLowerCase();
		if (!scName.endsWith(".bin")) file = new File(scName + ".bin");
		ObjectOutputStream streamOut = new ObjectOutputStream(new FileOutputStream(file));
		streamOut.writeObject(dataChar);
		streamOut.close();
		
		return true;
	}		
}

class ActionPerformSaveXML extends ActionPerformSave
{
	@Override
	protected boolean setFileFilter(JFileChooser fileChooser)
	{
		FileFilter[] aFilters = fileChooser.getChoosableFileFilters();
		for (FileFilter filter : aFilters) fileChooser.removeChoosableFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileFilter(new FileNameExtensionFilter("XML data files", "xml"));
		return true;
	}
	
	@Override
	protected boolean saveFile(CharacterData dataChar, File file) throws IOException
	{
		// TODO bug saves to parent folder
		String scName = file.getName().toLowerCase();
		if (!scName.endsWith(".xml")) file = new File(scName + ".xml");
		
		PersistHelper dataCharHelper = new PersistHelper();
		OutputStream streamOut = new FileOutputStream(file);
		dataCharHelper.saveDocument(dataChar, streamOut);
		streamOut.close();
		
		return true;
	}	
}

class ActionPerformMerge extends ActionPerformLoad
{
	ActionPerformMerge(CharacterListener listenerData)
	{
		m_listenerData = listenerData;
	}
	
	ActionPerformMerge(CharacterListener listenerData, String scFile)
	{
		m_listenerData = listenerData;
		m_scFile = scFile;
	}
	
	private CharacterListener m_listenerData = null;
	private String m_scFile = null;
	
	@Override
	protected boolean setMembers(CharacterData dataChar, Matcher matchRegex)
	{
		if (!matchRegex.find()) return false; // Find 'Merge'
		
		if (!matchRegex.find()) return true;
		m_scFile = removeQuotedGroup1(matchRegex);
		
		return true;
	}
	
	@Override
	protected boolean setFileFilter(JFileChooser fileChooser)
	{
		FileFilter[] aFilters = fileChooser.getChoosableFileFilters();
		for (FileFilter filter : aFilters) fileChooser.removeChoosableFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileFilter(new FileNameExtensionFilter("Text files for merge", "txt"));
		return true;
	}
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataChar)
	{
		// When merging data suppress event generation till merge is complete
		NotifyData notify = dataChar.getRoot();
		
		try
		{
			notify.suppressEvent(true);
			return mergeFile(componentParent, dataChar);
		}
		finally
		{
			notify.suppressEvent(false);			
		}
	}
	
	private boolean mergeFile(Component componentParent, CharacterData dataChar)
	{
		if (null==m_scFile)
		{
			int iResponse = showOpenDialog(componentParent);
			if (JFileChooser.APPROVE_OPTION!=iResponse) return false;
			File file = getSelectedFile();
			return loadFileAndClose(dataChar, file);
		}
		File file = getFileFromChosen(m_scFile);
		return loadFileAndClose(dataChar, file);			
	}
	
	@Override
	protected boolean loadFile(CharacterData dataCharacterExisting, File file) throws IOException
	{
		BufferedReader readerText = new BufferedReader(new FileReader(file));
		
		try 
		{
			CharacterData dataCharacterMerge = new CharacterData();
			dataCharacterMerge.addListener(m_listenerData);
			
			if (!interpretText(dataCharacterMerge, readerText)) return m_listenerData.failMessage(new CharacterEventError("Failed to interpret file "));
			//showTree(dataCharacterMerge, "Loaded");
			if (!mergeData(dataCharacterExisting, dataCharacterMerge)) return m_listenerData.failMessage(new CharacterEventError("Failed to merge file content "));
			//showTree(dataCharacterExisting, "Merged");
		} 
		finally
		{
			readerText.close();
		}
		
		return true;		
	}
	
	private boolean interpretText(CharacterData dataCharacterRead, BufferedReader readerText) throws IOException
	{
		ParseHelper dataParse = new ParseHelper(dataCharacterRead);
		
		for (String scLine = ""; null!=scLine; scLine = readerText.readLine())
		{
			if (!dataParse.addLine(scLine)) return false;
		}
		
		return true;
	}
	
	private boolean mergeData(CharacterData dataCharacterExisting, CharacterData dataCharacterMerge)
	{
		MergeHelper helperMerge = new MergeHelper(dataCharacterExisting);
		return helperMerge.mergeData(dataCharacterMerge);			
	}
}

class ActionPerformShow extends ActionPerform
{
	private RegionData m_dataRegion = null;
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataCharacter)
	{
		// Fire event from root since listeners for action might only register on the root
		NotifyData dataRegionRoot = dataCharacter.getRoot();
		CharacterEventShow eventShow = new CharacterEventShow(dataCharacter, m_dataRegion);
		return dataRegionRoot.fireCharacterEvent(eventShow);
	}
	
	@Override
	protected boolean setMembers(CharacterData dataChar, Matcher matchRegex)
	{
		if (!matchRegex.find()) return false; // Find 'Show'
		m_dataRegion = dataChar.getRoot();
		
		if (!matchRegex.find()) return true;		
		String scCategory = removeQuotedGroup1(matchRegex);
		CategoryData dataCategory = dataChar.getCategory(scCategory);
		if (null==dataCategory) return true;
		m_dataRegion = dataCategory;
		
		if (!matchRegex.find()) return true;		
		String scSelection = removeQuotedGroup2(matchRegex);		
		SelectionData dataSelection = (SelectionData)dataCategory.getRegion(scSelection);
		if (null==dataSelection) return true;
		m_dataRegion = dataSelection;

		return true;
	}
}

class ActionPerformSelect extends ActionPerform
{	
	ActionPerformSelect()
	{	
	}
	
	ActionPerformSelect(SelectionData dataSelection, boolean zSelected)
	{
		m_dataSelection = dataSelection;
		m_zSelected = zSelected;
	}
	
	private SelectionData m_dataSelection = null;
	private boolean m_zSelected = false;
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataCharacter)
	{
		NotifyData notifyData = dataCharacter.getRoot();
		return m_dataSelection.setSelected(notifyData, m_zSelected);
	}
	
	@Override
	protected boolean setMembers(CharacterData dataChar, Matcher matchRegex)
	{
		if (!matchRegex.find()) return false; // Find 'Select'
		
		if (!matchRegex.find()) return false;
		String scCategory = removeQuotedGroup1(matchRegex);
		CategoryData dataCategory = dataChar.getCategory(scCategory);
		if (null==dataCategory) return false;
		
		if (!matchRegex.find()) return false;		
		String scSelection = removeQuotedGroup2(matchRegex);		
		m_dataSelection = (SelectionData)dataCategory.getRegion(scSelection);
		if (null==m_dataSelection) return false;
		
		if (!matchRegex.find()) return false;
		String scSelected = matchRegex.group();
		m_zSelected = Boolean.parseBoolean(scSelected);
		
		return true;
	}
}

class ActionPerformValue extends ActionPerform
{
	ActionPerformValue()
	{	
	}
	
	ActionPerformValue(SelectionData dataSelection, String scValue)
	{
		m_dataSelection = dataSelection;
		m_scValue = scValue;
	}
	
	protected SelectionData m_dataSelection = null;
	protected String m_scValue = null;
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataCharacter)
	{
		Object valueSet = m_dataSelection.setData(new ValueDataText(m_scValue.toString()));
		return null!=valueSet;
	}
	
	@Override
	protected boolean setMembers(CharacterData dataChar, Matcher matchRegex)
	{
		if (!matchRegex.find()) return false; // Find 'Select'
		
		if (!matchRegex.find()) return false;
		String scCategory = removeQuotedGroup1(matchRegex);
		CategoryData dataCategory = dataChar.getCategory(scCategory);
		if (null==dataCategory) return false;
		
		if (!matchRegex.find()) return false;		
		String scSelection = removeQuotedGroup2(matchRegex);		
		m_dataSelection = (SelectionData)dataCategory.getRegion(scSelection);
		if (null==m_dataSelection) return false;
		
		if (!matchRegex.find()) return false;
		m_scValue = removeQuotedGroup1(matchRegex);
		
		return true;
	}	
}

class ActionPerformExpression extends ActionPerformValue
{
	ActionPerformExpression()
	{	
	}
	
	ActionPerformExpression(SelectionData dataSelection, String scValue)
	{
		super(dataSelection, scValue);
	}
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataCharacter)
	{
		Object valueResult = dataCharacter.getEvaluation().evaluateObject(m_dataSelection, m_scValue);
		Object valueSet = m_dataSelection.setData(new ValueDataText(valueResult.toString()));
		return null!=valueSet;
	}	
}

class ActionAbout extends ActionPerform
{
	ActionAbout()
	{	
	}
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataCharacter)
	{
		About about = new About();		
		return about.createPanel(componentParent);
	}
}

class ActionHelp extends ActionPerform
{
	ActionHelp(ActionListener listenerCallback)
	{
		m_listenerCallback = listenerCallback;
	}
	
	ActionListener m_listenerCallback = null;
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataCharacter)
	{
		Help help = new Help();		
		return help.createPanel(componentParent, m_listenerCallback);
	}
}

class ActionPerformLayout extends ActionPerform
{
	ActionPerformLayout()
	{
	}
	
	private boolean m_zShow = false;
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataCharacter)
	{
		RegionData m_dataRoot = dataCharacter.getRoot();
		CharacterEventLayout eventLayout = new CharacterEventLayout(dataCharacter, m_dataRoot, m_zShow);
		return m_dataRoot.fireCharacterEvent(eventLayout);
	}
	
	@Override
	protected boolean setMembers(CharacterData dataChar, Matcher matchRegex)
	{
		if (!matchRegex.find()) return false; // Find 'Select'
		
		if (!matchRegex.find()) return false; // Next need to have TRUE or FALSE
		
		String scShow = removeQuotedGroup1(matchRegex);
		m_zShow = Boolean.parseBoolean(scShow);
		
		return true;
	}
}

class ActionPerformNew extends ActionPerform
{
	ActionPerformNew()
	{
	}
	
	@Override
	public boolean actionPerform(Component componentParent, CharacterData dataCharacter)
	{
		// When merging data suppress event generation till merge is complete
		NotifyData notify = dataCharacter.getRoot();
		
		try
		{
			//notify.suppressEvent(true);
			return dataCharacter.removeAll();
		}
		finally
		{
			notify.suppressEvent(false);
			notify.fireCharacterEvent(new CharacterEventEdit(EventEditEnum._UPDATE_ALL));
		}
	}
}

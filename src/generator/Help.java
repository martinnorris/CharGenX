package generator;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

public class Help 
{
	public boolean createPanel(Component componentParent, ActionListener listenerAction)
	{
		JPanel panelHelp = new JPanel();
		panelHelp.setLayout(new BoxLayout(panelHelp, BoxLayout.PAGE_AXIS));
		panelHelp = buildPanel(panelHelp, listenerAction);
		
		return ViewHelper.framePopup(componentParent, panelHelp, "Character Generator from XML");
	}
	
	private JPanel buildPanel(final JPanel panelHelp, ActionListener listenerAction) 
	{
		JTextPane paneAbout = new JTextPane();
		paneAbout.setText("Loading file ...");
		
        JButton buttonOK = new JButton();
        buttonOK.setText("OK");
        buttonOK.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        ActionListener buttonActionListener = new ActionListener()
        {
        	@Override
			public void actionPerformed(ActionEvent event)
        	{
        		Window windowSource = SwingUtilities.getWindowAncestor(panelHelp);
        		WindowEvent eventClosing = new WindowEvent(windowSource, WindowEvent.WINDOW_CLOSING);
        		
        		windowSource.dispatchEvent(eventClosing);
        		windowSource.setVisible(false);
        		windowSource.dispose();
        		
        		return;
        	}
        };
        buttonOK.addActionListener(buttonActionListener);
        
        JButton buttonTree = new JButton();
        buttonTree.setText("Tree");
        buttonTree.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonTree.addActionListener(listenerAction);
        
        JToggleButton buttonLayout = new JToggleButton()
		{
			private static final long serialVersionUID = 3797910817818860826L;

			@Override
			protected void fireActionPerformed(ActionEvent event) 
			{
        		boolean zShow = isSelected();
        		ActionEvent eventAdapted = new ActionEvent(this, event.getID(), String.format("Layout %s", zShow?"TRUE":"FALSE"));
				super.fireActionPerformed(eventAdapted);
			}
		};
		
        buttonLayout.setText("Layout");
        buttonLayout.setAlignmentX(Component.CENTER_ALIGNMENT);        
        buttonLayout.addActionListener(listenerAction);
        
        try
        {
			// Get the resource from the class loader so that it searches the whole class path (and resource is in class path /resource directory which is NOT the same as the class files
			URL htmlHelp = getClass().getClassLoader().getResource("help.html");
			paneAbout.setPage(htmlHelp);
        }
        catch (IOException x)
        {
        	paneAbout.setText("... could not find pretty 'Help' file, just to let you know");
			x.printStackTrace();
        }
        
        JPanel panelButtons = new JPanel();
        panelButtons.add(buttonTree);
        panelButtons.add(buttonLayout);
        panelButtons.add(buttonOK);
        
        JScrollPane paneScroll = new JScrollPane(paneAbout);
        paneScroll.setPreferredSize(new Dimension(350, 250));
        
        panelHelp.add(paneScroll);
        panelHelp.add(panelButtons);
        
		return panelHelp;
	}
}

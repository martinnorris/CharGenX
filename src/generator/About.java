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
import javax.swing.SwingUtilities;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class About 
{
	public boolean createPanel(Component componentParent)
	{
		JPanel panelAbout = new JPanel();
		panelAbout.setLayout(new BoxLayout(panelAbout, BoxLayout.PAGE_AXIS));
		panelAbout = buildPanel(panelAbout);
		
		return ViewHelper.framePopup(componentParent, panelAbout, "Character Generator from XML");
	}

	private JPanel buildPanel(final JPanel panelAbout) 
	{
		JTextPane paneAbout = new JTextPane();
		//paneAbout.setEditorKit(new WrapEditorKit());
		paneAbout.setText("Loading file ...");
		
        JButton buttonOK = new JButton();
        buttonOK.setText("OK");
        buttonOK.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        ActionListener buttonActionListener = new ActionListener()
        {
        	@Override
			public void actionPerformed(ActionEvent event)
        	{
        		Window windowSource = SwingUtilities.getWindowAncestor(panelAbout);
        		WindowEvent eventClosing = new WindowEvent(windowSource, WindowEvent.WINDOW_CLOSING);
        		
        		windowSource.dispatchEvent(eventClosing);
        		windowSource.setVisible(false);
        		windowSource.dispose();
        		
        		return;
        	}
        };
        buttonOK.addActionListener(buttonActionListener);
        
        try
        {
			// Get the resource from the class loader so that it searches the whole class path (and resource is in class path /resource directory which is NOT the same as the class files
			URL htmlAbout = getClass().getClassLoader().getResource("about.html");
			paneAbout.setPage(htmlAbout);
        }
        catch (IOException x)
        {
        	paneAbout.setText("... could not find pretty 'About' file, just to let you know");
			x.printStackTrace();
        }
        
        JScrollPane paneScroll = new JScrollPane(paneAbout);
        paneScroll.setPreferredSize(new Dimension(350, 250));
        
        panelAbout.add(paneScroll);
        panelAbout.add(buttonOK);
        
		return panelAbout;
	}
}

class WrapEditorKit extends HTMLEditorKit implements ViewFactory
{	
	private static final long serialVersionUID = 7978854973800621943L;

	private ViewFactory m_defaultFactory = null;

    @Override
    public ViewFactory getViewFactory() 
    {
		m_defaultFactory = super.getViewFactory();
        return this;
    }

	@Override
	public View create(Element element) 
	{
		View viewDefault = m_defaultFactory.create(element);
		// If it is not a label then nothing to do
		if (!(viewDefault instanceof LabelView)) return viewDefault;

		Object o = element.getAttributes().getAttribute(StyleConstants.NameAttribute);
		// If the label is for a tag or a break then return the default
		if ((o instanceof HTML.Tag) && (o == HTML.Tag.BR)) return viewDefault;
		// Create a labal that breaks
		return new WrapLabelView(element);
	}
}

class WrapLabelView extends LabelView 
{
    public WrapLabelView(Element element) 
    {
        super(element);
    }

    @Override
    public float getMinimumSpan(int iAxis) 
    {
        switch (iAxis) 
        {
            case View.X_AXIS:
                return 0;
            case View.Y_AXIS:
                return super.getMinimumSpan(iAxis);
            default:
                throw new IllegalArgumentException("Invalid view axis value " + Integer.toString(iAxis));
        }
    }
}

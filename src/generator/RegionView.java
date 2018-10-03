package generator;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public interface RegionView 
{
	public JComponent buildPanel(ViewHelper helperView, RegionData dataRegion, JComponent componentContainer);
}

class RegionViewSingle implements RegionView
{
	@Override
	public JComponent buildPanel(ViewHelper helperView, RegionData dataRegion, JComponent componentContainer)
	{
		// For only a single contained region build in the parent
		return componentContainer;
	}	
}

class RegionViewPanel extends JPanel
{
	private static final long serialVersionUID = -7013943788766197946L;

	RegionViewPanel(String scName)
	{
		m_scName = scName;
	}
	
	private String m_scName = null;
	
	@Override
	public String toString()
	{
		return m_scName;
	}				
}

class RegionViewBase implements RegionView
{
	@Override
	public JComponent buildPanel(ViewHelper helperView, RegionData dataRegion, JComponent componentContainer)
	{
		return buildPanel(helperView, dataRegion, componentContainer, BoxLayout.PAGE_AXIS, Box.createGlue());
	}
	
	private JComponent buildPanel(ViewHelper helperView, RegionData dataRegion, JComponent componentContainer, int iAxis, Component componentGlue)
	{
		JPanel panelContent = new RegionViewPanel(String.format("Region %s panel", dataRegion.toString()));
		panelContent.setLayout(new BoxLayout(panelContent, iAxis));
		if (!helperView.buildContent(dataRegion, panelContent)) return componentContainer;
		panelContent.add(componentGlue);

		JScrollPane paneRegion = new JScrollPane(panelContent);
		componentContainer.add(paneRegion);
		
		return panelContent;		
	}
}


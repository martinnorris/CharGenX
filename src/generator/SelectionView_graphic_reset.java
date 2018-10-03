package generator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

public class SelectionView_graphic_reset extends SelectionViewGraphicBase
{
	private static final long serialVersionUID = 4922771586107092647L;

	@Override
	public JComponent updatePanel(SelectionData dataSelection, Options optionsView, String scHints) 
	{
		// Do not set colour {cos it will be the foreground colour}
		return this;
	}
		
	@Override
	public void paintIcon(Component componentContainer, Graphics g, int iX, int iY) 
	{
		Graphics2D g2d = (Graphics2D)g.create();
		
		Color colourSelection = Color.BLACK; //SelectionViewBase.chooseColour(m_optionsView, m_dataSelection, Color.BLACK);
		g2d.setColor(colourSelection);
		
		Dimension dimensionPanel = componentContainer.getSize();
		int iSizeX = dimensionPanel.width;
		int iSizeY = dimensionPanel.height;
		iSizeX = iSizeY = Math.min(iSizeX, iSizeY);
		
		BasicStroke stroke = new BasicStroke(iSizeX/8);
		g2d.setStroke(stroke);
		
		int iPlaceX1 = iSizeX * 3 / 8;
		int iPlaceX2 = iSizeX * 5 / 8;
		
		g2d.drawLine(iPlaceX1, iSizeY * 2 / 8, iPlaceX1, iSizeY * 6 / 8);
		g2d.drawLine(iPlaceX2, iSizeY * 2 / 8, iPlaceX2, iSizeY * 6 / 8);
		
		g2d.dispose();
	}	
}
package generator;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class SelectionView_graphic_up extends SelectionViewGraphicBase
{
	private static final long serialVersionUID = -6327388329187580896L;

	@Override
	public void paintIcon(Component componentContainer, Graphics g, int iX, int iY) 
	{
		Color colourSelection = Color.BLACK; //SelectionViewBase.chooseColour(m_optionsView, m_dataSelection, Color.BLACK);
		g.setColor(colourSelection);
		
		Dimension dimensionPanel = componentContainer.getSize();
		int iSizeX = dimensionPanel.width;
		int iSizeY = dimensionPanel.height;
		iSizeX = iSizeY = Math.min(iSizeX, iSizeY);
		
		int[] aiXPoints = 
			{
				iSizeX/2, iSizeX/10, iSizeX * 9 / 10,
			};
		int[] aiYPoints = 
			{
				iSizeY/10, iSizeY * 9 / 10, iSizeY * 9 / 10,					
			};
		
		g.fillPolygon(aiXPoints, aiYPoints, 3);
	}
}
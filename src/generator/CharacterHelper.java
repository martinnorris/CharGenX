package generator;

import javax.swing.JComponent;

public interface CharacterHelper
{
	public boolean persistData(PersistHelper helperPersist);
	
	public boolean persistContent(PersistHelper helperPersist);
	
	public boolean restoreContent(ParseHelper helperParse, String scType, boolean zStart, boolean zEnd, String scContent);
	
	public boolean handleView(ViewHelper helperView, JComponent componentView);
	
	// First dataRegion is to pass parent in situation where child needs to be recreated in the scope of the parent
	public boolean handleMerge(RegionData dataRegionParent, MergeHelper helperMerge, CharacterData dataCharacterMerge, RegionData dataRegionMerge);
}
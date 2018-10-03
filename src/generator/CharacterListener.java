package generator;

public interface CharacterListener
{
	public boolean changeView(CharacterEventView eventCharacter);
	
	public boolean madeEdit(CharacterEventEdit eventCharacter);
	
	public boolean madeSelection(CharacterEventSelect eventCharacter);

	public boolean failMessage(CharacterEventError eventCharacter);
}

class CharacterListenerAdapter implements CharacterListener
{
	@Override
	public boolean changeView(CharacterEventView eventCharacter) 
	{
		return true;
	}	
	
	@Override
	public boolean madeEdit(CharacterEventEdit eventEdit) 
	{
		return true;
	}

	@Override
	public boolean madeSelection(CharacterEventSelect eventSelect) 
	{
		return true;
	}

	@Override
	public boolean failMessage(CharacterEventError eventError) 
	{
		return true;
	}
}

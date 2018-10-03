package generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

class PersistHelper extends XMLFilterImpl
{
	private CharacterData m_dataChar = null;
	
	private ContentHandler m_handlerDocument = null;
	private Attributes m_attributesDocument = null;
	
	private ParseHelper m_parseHelper = null;
	private String m_scElement = null;
	private StringBuilder m_sbContent = null;
	
	private class AttributePair
	{
		private AttributePair(String scAttribute, String scValue)
		{
			m_scAttribute = scAttribute;
			m_scValue = scValue;
		}
		
		private String m_scAttribute = null;
		private String m_scValue = null;
		
		public RegionData acceptPreset(String scType) 
		{
			return m_parseHelper.setElement(scType, m_scAttribute, m_scValue);
		}
	}
	
	private class AttributeList extends ArrayList<AttributePair>
	{
		private static final long serialVersionUID = 1L;

		public AttributeList()
		{
			
		}
		
		private void populateList(Attributes attributes)
		{
			clear();
			
			int iCount = attributes.getLength();
			
			for (int iIndex = 0; iIndex<iCount; ++iIndex)
			{
				String scAttribute = attributes.getLocalName(iIndex);
				String scValue = attributes.getValue(iIndex);
				m_ListAttributePairs.add(new AttributePair(scAttribute, scValue));
			}
			
			return;			
		}
		
		private boolean interpretList(String scType)
		{
			for (AttributePair pairAttribute : this)
			{
				RegionData dataRegion = pairAttribute.acceptPreset(scType);
				if (null==dataRegion) continue;
				if (!(dataRegion instanceof SelectionData)) continue;
				SelectionData dataSelection = (SelectionData)dataRegion;
				m_Presets.addPreset(dataSelection);
			}
				
			clear();	
			return true;
		}
	}
	
	private AttributeList m_ListAttributePairs = null;
	private CharacterModelPresets m_Presets = null; 
	
	public CharacterData loadDocument(InputStream streamIn, CharacterListener listListener)
	{
		// Create a new document and listen to trace things added
		CharacterData dataCharacterReturn = new CharacterData();
		dataCharacterReturn.addListener(listListener);
		
		m_parseHelper = new ParseHelper(dataCharacterReturn);
		InputSource sourceXML = new InputSource(streamIn);
		
		try
		{
			XMLReader readerXML = XMLReaderFactory.createXMLReader();
			readerXML.setContentHandler(this);
			readerXML.setErrorHandler(this);
			readerXML.parse(sourceXML);
			
			m_Presets.presetValues(dataCharacterReturn.getRoot());
		}
		catch (SAXException x) 
		{
			throw new RuntimeException("Could not interpret XML document" + x.toString());
		}
		catch (IOException x) 
		{
			throw new RuntimeException("Could not load XML document" + x.toString());
		}
		
		return dataCharacterReturn;		
	}
	
	// When loading the parser generated events that trigger methods to populate document
	
	@Override
	public void startDocument() throws SAXException 
	{
		m_sbContent = new StringBuilder();
		m_ListAttributePairs = new AttributeList();		
		m_Presets = new CharacterModelPresets();
	}

	@Override
	public void startElement(String scURI, String localName, String scQualifiedName, Attributes attributes) throws SAXException 
	{
		// Parse the previous tag and data
		m_parseHelper.contentElement(m_scElement, m_sbContent.toString());
		m_ListAttributePairs.interpretList(m_scElement);
		
		// Start of new tag
		m_scElement = scQualifiedName;
		m_sbContent.setLength(0);
		m_ListAttributePairs.populateList(attributes);
		
		m_parseHelper.startElement(scQualifiedName);
	}	
	
	@Override
	public void characters(char[] aChars, int iStart, int iLength) throws SAXException 
	{
		// Exclude '/n' in characters
		for (int iIndex = 0; iIndex<iLength; ++iIndex)
		{
			if ('\n'==aChars[iIndex+iStart]) continue;
			m_sbContent.append(aChars[iIndex+iStart]);			
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String scQualifiedName) throws SAXException 
	{
		m_parseHelper.contentElement(scQualifiedName, m_sbContent.toString());
		m_ListAttributePairs.interpretList(scQualifiedName);
		
		m_parseHelper.endElement(scQualifiedName);
		
		m_scElement = null;
		m_sbContent.setLength(0);
	}
	
	

	public boolean saveDocument(CharacterData dataOut, OutputStream streamOut)
	{
		m_dataChar = dataOut;
		m_attributesDocument = new AttributesImpl();
			
		try
		{
			TransformerFactory factoryDocumentTransformer = TransformerFactory.newInstance();
			Transformer transformerDocument = factoryDocumentTransformer.newTransformer();
			transformerDocument.setOutputProperty(OutputKeys.INDENT, "yes");
			
			SAXSource sourceDocument = new SAXSource(this, null); // Does not need input stream since uses data from document
			StreamResult resultStream = new StreamResult(streamOut);
			
			transformerDocument.transform(sourceDocument, resultStream);
		}
		catch (TransformerException x) 
		{
			throw new RuntimeException("Could not save XML document" + x.toString());
		}
		
		return true;
	}
	
	@Override
	public void setContentHandler(ContentHandler handler)
	{
		m_handlerDocument = handler;
	}
	
	@Override
	public ContentHandler getContentHandler()
	{
		return m_handlerDocument;
	}
	
	/**
	 * The persistence is handled by call backs
	 * Each data class implements handleData with arguments that are instance of this helper class
	 * And this calls back into this helper class with the correct method
	 */
	
	@Override
	public void parse(InputSource input)
	{
		m_dataChar.persistData(this);
	}
	
	public boolean handleData(CharacterData dataChar, String scIdentifier)
	{
		try
		{
			m_handlerDocument.startDocument();
			persistComment("<!-- XML generated by CharGenX PersistHelper -->");
			m_handlerDocument.startElement("", scIdentifier, scIdentifier, m_attributesDocument);
			
			dataChar.persistContent(this);
			
			m_handlerDocument.endElement("", scIdentifier, scIdentifier);
			m_handlerDocument.endDocument();
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not merge XML document" + x.toString());
			
		}
		
		return true;
	}
	
	public boolean handleRegion(String scRegion, RegionData dataRegion, String scIdentifier)
	{
		try
		{
			m_handlerDocument.startElement("", scIdentifier, scIdentifier, m_attributesDocument);
			persistContent(scRegion);
			
			dataRegion.persistContent(this);
			
			m_handlerDocument.endElement("", scIdentifier, scIdentifier);
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not merge XML document" + x.toString());			
		}
		
		return true;
	}
	
	public boolean handleCategory(String scCategory, CategoryData dataCategory, String scIdentifier)
	{
		try
		{
			m_handlerDocument.startElement("", scIdentifier, scIdentifier, m_attributesDocument);
			persistContent(scCategory);
			
			dataCategory.persistContent(this);
			
			m_handlerDocument.endElement("", scIdentifier, scIdentifier);
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not merge XML document" + x.toString());			
		}
		
		return true;		
	}
	
	public boolean handleSelection(String scSelection, SelectionData dataSelection, String scIdentifier)
	{
		try
		{
			AttributesImpl attributesSelection = new AttributesImpl();
			attributesSelection.addAttribute("", "selected", "selected", "BOOLEAN", Boolean.toString(dataSelection.isSelected()));
			attributesSelection.addAttribute("", "suggested", "suggested", "BOOLEAN", Boolean.toString(dataSelection.isSuggested()));
			attributesSelection.addAttribute("", "excluded", "excluded", "BOOLEAN", Boolean.toString(dataSelection.isExcluded()));
			
			m_handlerDocument.startElement("", scIdentifier, scIdentifier, attributesSelection);
			persistContent(scSelection);
			
			dataSelection.persistContent(this);
			
			m_handlerDocument.endElement("", scIdentifier, scIdentifier);
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not merge XML document" + x.toString());			
		}
		
		return true;
	}
	
	public boolean handleSelectionElement(String scElement, RegionData dataRegion)
	{
		try
		{
			m_handlerDocument.startElement("", scElement, scElement, m_attributesDocument);
			dataRegion.persistData(this);
			m_handlerDocument.endElement("", scElement, scElement);
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not merge XML document" + x.toString());			
		}
		
		return true;
	}
	
	public boolean handleSelectionList(SelectionData dataSelection, String scList)
	{
		String scListCategory = CategoryData.sm_scIdentifier;
		String scListSelection = SelectionData.sm_scIdentifier;
		
		CategoryData dataCategory = dataSelection.getParent();
		String scCategory = dataCategory.getName();
		String scSelection = dataSelection.toString();
		
		try
		{
			m_handlerDocument.startElement("", scList, scList, m_attributesDocument);
			handleElement(scListCategory, scCategory);
			handleElement(scListSelection, scSelection);
			m_handlerDocument.endElement("", scList, scList);
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not merge XML document" + x.toString());			
		}
		
		return true;
	}
	
	public boolean handleCategoryList(CategoryData dataCategory, String scList)
	{
		String scListCategory = CategoryData.sm_scIdentifier;
		String scCategory = dataCategory.getName();
				
		try
		{
			m_handlerDocument.startElement("", scList, scList, m_attributesDocument);
			handleElement(scListCategory, scCategory);
			m_handlerDocument.endElement("", scList, scList);
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not add list element to XML document" + x.toString());			
		}
		
		return true;
	}
	
	public boolean handleElement(String scName, String scValue)
	{
		try
		{
			m_handlerDocument.startElement("", scName, scName, m_attributesDocument);
			persistContent(scValue);
			m_handlerDocument.endElement("", scName, scName);
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not add list element to XML document" + x.toString());			
		}
		
		return true;		
	}
	
	private boolean persistComment(String scComment)
	{
		try 
		{
			m_handlerDocument.processingInstruction(StreamResult.PI_DISABLE_OUTPUT_ESCAPING, "");
			persistContent(scComment);
			m_handlerDocument.processingInstruction(StreamResult.PI_ENABLE_OUTPUT_ESCAPING, "");
		} 
		catch (SAXException x) 
		{
			throw new RuntimeException("Could not write comment to XML document" + x.toString());			
		}
		
		return true;
	}
	
	private boolean persistContent(String scContent)
	{
		char[] acBreak = {'\n'};
		
		try
		{
			char[] acContent = scContent.toCharArray();
			
			m_handlerDocument.characters(acBreak, 0, 1);
			m_handlerDocument.characters(acContent, 0, acContent.length);			
			m_handlerDocument.characters(acBreak, 0, 1);
		}
		catch (SAXException x)
		{
			throw new RuntimeException("Could not add break to XML document" + x.toString());			
		}
		
		return true;	
	}
}
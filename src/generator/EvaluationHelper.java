package generator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParseArgument
{
	public ParseArgument(String scValue)
	{
		m_scValue = scValue;
		m_iIndex = 0;
		// Regex to separate "A B C" 'a b c' A B C a.b.c 
		// e.g. "A B C" 'a b c' A B C a.b.c tokens 'A B C' 'a b c' 'A' 'B' 'C' 'a' 'b' 'c'
		String scRegex = "\"([^\"]*)\"|'([^']*)'|([^\"' .]+)";
		m_patternRegex = Pattern.compile(scRegex);
	}
	
	private String m_scValue;
	private int m_iIndex;
	private Pattern m_patternRegex = null;
	
	private boolean m_zDecimal = false;
	private boolean m_zCurrency = false;
	
	public boolean hasSymbol()
	{
		while (m_iIndex<m_scValue.length())
		{
			if (Character.isWhitespace(m_scValue.charAt(m_iIndex))) 
				++m_iIndex;
			else
				return true;
		}
		return false;
	}
	
	public int nextChar()
	{
		while (m_iIndex<m_scValue.length())
		{
			if (Character.isWhitespace(m_scValue.charAt(m_iIndex))) 
				++m_iIndex;
			else
				return m_scValue.charAt(m_iIndex);
		}
		return EvaluationHelper.END_PARSING;
	}
	
	public int lastChar()
	{
		if (1>m_iIndex) return m_scValue.charAt(0);
		return m_scValue.charAt(m_iIndex-1);
	}
	
	public ParseArgument consume(int iNumber)
	{
		m_iIndex += iNumber;
		return this;
	}
	
	public boolean isNumberNext()
	{
		int characterTest = nextChar();
		return Character.isDigit(characterTest);
	}
	
	public boolean isDecimal()
	{
		return m_zDecimal;
	}
	
	public boolean isCurrency()
	{
		return m_zCurrency;
	}
	
	public String getNumber()
	{
		int iStart = m_iIndex;
		m_zDecimal = false;
		
		while (m_iIndex<m_scValue.length())
		{
			if ('.'==m_scValue.charAt(m_iIndex))
			{
				m_zDecimal = true;
				++m_iIndex;
				continue;
			}
			if ('|'==m_scValue.charAt(m_iIndex))
			{
				// '|' divides pounds/shillings/pence
				m_zCurrency = true;
				++m_iIndex;
				continue;
			}
			if (Character.isDigit(m_scValue.charAt(m_iIndex)))
			{
				++m_iIndex;
				continue;
			}
			break;
		}
		
		return m_scValue.substring(iStart, m_iIndex);
	}
	
	public String getStringRegex()
	{
		Matcher matchRegex = m_patternRegex.matcher(m_scValue);
		if (!matchRegex.find(m_iIndex)) return null;
		m_iIndex = matchRegex.end();
		
		String scReturn = matchRegex.group();		
		if ('"'==scReturn.charAt(0)) return matchRegex.group(1);
		if ('\''==scReturn.charAt(0)) return matchRegex.group(2);
		
		return scReturn;
	}

	public String remainder() 
	{
		if (m_scValue.length()<m_iIndex) return "";
		return m_scValue.substring(m_iIndex);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(m_scValue);
		sb.append(' ');
		sb.append(m_iIndex);
		sb.append(' ');
		return sb.toString();
	}

	public String partEnding(char c) 
	{
		int iIndex = m_scValue.indexOf(c, m_iIndex);
		if (m_iIndex>=iIndex) return "";
		String scReturn = m_scValue.substring(m_iIndex, iIndex-1);
		m_iIndex = iIndex;
		return scReturn;
	}
}


class EvaluationHelper 
{
	public EvaluationHelper(CharacterData dataCharacter)
	{
		m_dataCharacter = dataCharacter;
	}
	
	private CharacterData m_dataCharacter = null;
	public static final int END_PARSING = -1;
	
	public static EvaluationHelper newEvaluationHelper(CharacterData dataCharacter)
	{
		sm_helperPrevious = new EvaluationHelper(dataCharacter);
		return sm_helperPrevious;
	}
	
	public static EvaluationHelper currentEvaluationHelper()
	{
		return sm_helperPrevious;
	}
	
	private static EvaluationHelper sm_helperPrevious = null;
	
	private enum ParseResultType {_UNSET, _REFERENCE, _STRING, _INTEGER, _DOUBLE};
	
	private abstract class ParseResult
	{
		private ParseResult(ParseResultType type)
		{
			m_iType = type.ordinal();
		}
		
		private int m_iType;
		
		abstract protected ParseResult copyResult();
		
		private ParseResult promote(ParseResult resultPromote)
		{
			try
			{
				if (resultPromote.m_iType<m_iType) return convert(resultPromote);
			}
			catch (NumberFormatException x)
			{
				// Cannot promote
			}
			return resultPromote;
		}
		
		private ParseResult compareTrue(ParseResult resultCompare)
		{
			ParseResult resultRight = promote(resultCompare);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.compareTrueResult(resultRight);			
		}
		
		private ParseResult compareEQ(ParseResult resultCompare)
		{
			ParseResult resultRight = promote(resultCompare);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.compareEQResult(resultRight);			
		}
		
		private ParseResult compareNE(ParseResult resultCompare)
		{
			ParseResult resultRight = promote(resultCompare);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.compareNEResult(resultRight);			
		}
		
		private ParseResult compareGT(ParseResult resultCompare)
		{
			ParseResult resultRight = promote(resultCompare);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.compareGTResult(resultRight);			
		}
		
		private ParseResult compareLT(ParseResult resultCompare)
		{
			ParseResult resultRight = promote(resultCompare);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.compareLTResult(resultRight);			
		}
		
		private ParseResult compareGE(ParseResult resultCompare)
		{
			ParseResult resultRight = promote(resultCompare);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.compareGEResult(resultRight);			
		}
		
		private ParseResult compareLE(ParseResult resultCompare)
		{
			ParseResult resultRight = promote(resultCompare);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.compareLEResult(resultRight);			
		}
		
		private ParseResult add(ParseResult resultAdd) throws EvaluationHelperException
		{
			ParseResult resultRight = promote(resultAdd);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.addResult(resultRight);
		}
		
		private ParseResult sub(ParseResult resultSub)
		{
			ParseResult resultRight = promote(resultSub);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.subResult(resultRight);			
		}
		
		private ParseResult multiply(ParseResult resultMultiply)
		{
			ParseResult resultRight = promote(resultMultiply);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.multiplyResult(resultRight);			
		}
		
		public ParseResult divide(ParseResult resultDivide)
		{
			ParseResult resultRight = promote(resultDivide);
			ParseResult resultLeft = resultRight.promote(this);
			return resultLeft.divideResult(resultRight);			
			
		}
		
		abstract protected ParseResult convert(ParseResult resultPromote);
		
		abstract protected ParseResult compareTrueResult(ParseResult resultCompare);
		abstract protected ParseResult compareEQResult(ParseResult resultCompare);
		abstract protected ParseResult compareNEResult(ParseResult resultCompare);
		abstract protected ParseResult compareGTResult(ParseResult resultCompare);
		abstract protected ParseResult compareLTResult(ParseResult resultCompare);
		abstract protected ParseResult compareGEResult(ParseResult resultCompare);
		abstract protected ParseResult compareLEResult(ParseResult resultCompare);
		abstract protected ParseResult addResult(ParseResult resultAdd) throws EvaluationHelperException;
		abstract protected ParseResult subResult(ParseResult resultSub);
		abstract protected ParseResult multiplyResult(ParseResult resultMultiply);
		abstract protected ParseResult divideResult(ParseResult resultDivide);
		
		abstract protected int toInteger();
		abstract protected double toDouble();
	}
	
	class ParseResultInteger extends ParseResult
	{
		public ParseResultInteger(int iValue)
		{
			super(ParseResultType._INTEGER);
			m_int = iValue;
		}
		
		private int m_int = 0;
		
		@Override
		protected ParseResult copyResult() 
		{
			return new ParseResultInteger(m_int);
		}
		
		@Override
		protected ParseResult compareTrueResult(ParseResult resultCompare)
		{
			if (m_int>0) return resultCompare;
			m_int = 0;
			return this;			
		}
		
		@Override
		protected ParseResult compareEQResult(ParseResult resultCompare)
		{
			if (m_int==resultCompare.toInteger())
				m_int = 1;
			else
				m_int = 0;
			return this;			
		}
		
		@Override
		protected ParseResult compareNEResult(ParseResult resultCompare)
		{
			if (m_int!=resultCompare.toInteger())
				m_int = 1;
			else
				m_int = 0;
			return this;			
		}
		
		@Override
		protected ParseResult compareGTResult(ParseResult resultCompare)
		{
			if (m_int>resultCompare.toInteger())
				m_int = 1;
			else
				m_int = 0;
			return this;			
		}
		
		@Override
		protected ParseResult compareLTResult(ParseResult resultCompare)
		{
			if (m_int<resultCompare.toInteger())
				m_int = 1;
			else
				m_int = 0;
			return this;			
		}
		
		@Override
		protected ParseResult compareGEResult(ParseResult resultCompare)
		{
			if (m_int>=resultCompare.toInteger())
				m_int = 1;
			else
				m_int = 0;
			return this;			
		}
		
		@Override
		protected ParseResult compareLEResult(ParseResult resultCompare)
		{
			if (m_int<=resultCompare.toInteger())
				m_int = 1;
			else
				m_int = 0;
			return this;			
		}
		
		@Override
		protected ParseResult addResult(ParseResult resultAdd)
		{
			m_int += resultAdd.toInteger();
			return this;
		}
		
		@Override
		protected ParseResult subResult(ParseResult resultSub)
		{
			m_int -= resultSub.toInteger();
			return this;
		}
		
		@Override
		protected ParseResult multiplyResult(ParseResult resultMultiply)
		{
			m_int *= resultMultiply.toInteger();
			return this;
		}
		
		@Override
		protected ParseResult divideResult(ParseResult resultDivide)
		{
			int iDivide = resultDivide.toInteger();
			
			if (0==iDivide)
			{
				return new ParseResultString("Too big");
			}
			
			int iMod = m_int % iDivide;
			
			if (0==iMod)
			{
				m_int /= iDivide;
				return this;				
			}
			
			ParseResultDouble resultReturn = new ParseResultDouble(m_int);
			return resultReturn.divide(resultDivide);
		}
		
		@Override
		protected ParseResult convert(ParseResult resultPromote)
		{
			return new ParseResultInteger(resultPromote.toInteger());
		}
		
		@Override
		protected int toInteger()
		{
			return m_int;
		}
		
		@Override
		protected double toDouble()
		{
			return (double)m_int;
		}
			
		@Override
		public String toString()
		{
			return Integer.toString(m_int);
		}
	}
	
	private class ParseResultDouble extends ParseResult
	{
		private ParseResultDouble(int iValue)
		{
			super(ParseResultType._DOUBLE);
			m_double = (double)iValue;
		}
		
		private ParseResultDouble(double dValue)
		{
			super(ParseResultType._DOUBLE);
			m_double = dValue;			
		}
		
		private double m_double = 0.0;
		
		@Override
		protected ParseResult copyResult() 
		{
			return new ParseResultDouble(m_double);
		}
		
		@Override
		protected ParseResult compareTrueResult(ParseResult resultCompare)
		{
			if (m_double>0.0) return resultCompare;
			return new ParseResultInteger(0);			
		}
		
		@Override
		protected ParseResult compareEQResult(ParseResult resultCompare)
		{
			if (m_double==resultCompare.toDouble()) return new ParseResultInteger(1);
			return new ParseResultInteger(0);			
		}
		
		@Override
		protected ParseResult compareNEResult(ParseResult resultCompare)
		{
			if (m_double!=resultCompare.toDouble()) return new ParseResultInteger(1);
			return new ParseResultInteger(0);			
		}
		
		@Override
		protected ParseResult compareGTResult(ParseResult resultCompare)
		{
			if (m_double>resultCompare.toDouble()) return new ParseResultInteger(1);
			return new ParseResultInteger(0);			
		}
		
		@Override
		protected ParseResult compareLTResult(ParseResult resultCompare)
		{
			if (m_double<resultCompare.toDouble()) return new ParseResultInteger(1);
			return new ParseResultInteger(0);			
		}
		
		@Override
		protected ParseResult compareGEResult(ParseResult resultCompare)
		{
			if (m_double>=resultCompare.toDouble()) return new ParseResultInteger(1);
			return new ParseResultInteger(0);			
		}
		
		@Override
		protected ParseResult compareLEResult(ParseResult resultCompare)
		{
			if (m_double<=resultCompare.toDouble()) return new ParseResultInteger(1);
			return new ParseResultInteger(0);			
		}
		
		@Override
		protected ParseResult addResult(ParseResult resultAdd)
		{
			m_double += resultAdd.toDouble();
			return this;
		}
		
		@Override
		protected ParseResult subResult(ParseResult resultSub)
		{
			m_double -= resultSub.toDouble();
			return this;
		}
		
		@Override
		protected ParseResult multiplyResult(ParseResult resultMultiply)
		{
			m_double *= resultMultiply.toDouble();
			return this;
		}
		
		@Override
		protected ParseResult divideResult(ParseResult resultDivide)
		{
			m_double /= resultDivide.toDouble();
			return this;
		}
		
		@Override
		protected ParseResult convert(ParseResult resultPromote)
		{
			return new ParseResultDouble(resultPromote.toDouble());
		}
		
		@Override
		protected int toInteger()
		{
			throw new RuntimeException("Should not convert from double to integer");
		}
		
		@Override
		protected double toDouble()
		{
			return m_double;
		}
		
		@Override
		public String toString()
		{
			return Double.toString(m_double);
		}		
	}
	
	private class ParseResultString extends ParseResult
	{
		private  ParseResultString(String scValue)
		{
			super(ParseResultType._STRING);
			m_scValue = scValue;
		}
		
		private String m_scValue = null;
		
		@Override
		protected ParseResult copyResult() 
		{
			return new ParseResultString(m_scValue);
		}
		
		@Override
		protected int toInteger()
		{
			return Integer.parseInt(m_scValue);
		}
		
		@Override
		protected double toDouble()
		{
			return Double.parseDouble(m_scValue);
		}
		
		@Override
		public String toString()
		{
			return m_scValue;
		}

		@Override
		protected ParseResult convert(ParseResult resultPromote) 
		{
			throw new RuntimeException("Cannot convert to reference type");
		}

		@Override
		protected ParseResult compareTrueResult(ParseResult resultCompare) 
		{
			return new ParseResultInteger(m_scValue.length()>0?1:0);
		}

		@Override
		protected ParseResult compareEQResult(ParseResult resultCompare) 
		{
			if (m_scValue.equals(resultCompare.toString())) return new ParseResultInteger(1);
			return new ParseResultInteger(0);
		}

		@Override
		protected ParseResult compareNEResult(ParseResult resultCompare) 
		{
			if (m_scValue.equals(resultCompare.toString())) return new ParseResultInteger(0);
			return new ParseResultInteger(1);
		}

		@Override
		protected ParseResult compareGTResult(ParseResult resultCompare) 
		{
			int iCompare = m_scValue.compareTo(resultCompare.toString());
			if (0<iCompare) return new ParseResultInteger(1);
			return new ParseResultInteger(0);
		}

		@Override
		protected ParseResult compareLTResult(ParseResult resultCompare) 
		{
			int iCompare = m_scValue.compareTo(resultCompare.toString());
			if (0>iCompare) return new ParseResultInteger(1);
			return new ParseResultInteger(0);
		}

		@Override
		protected ParseResult compareGEResult(ParseResult resultCompare) 
		{
			int iCompare = m_scValue.compareTo(resultCompare.toString());
			if (0>=iCompare) return new ParseResultInteger(1);
			return new ParseResultInteger(0);
		}

		@Override
		protected ParseResult compareLEResult(ParseResult resultCompare) 
		{
			int iCompare = m_scValue.compareTo(resultCompare.toString());
			if (0<=iCompare) return new ParseResultInteger(1);
			return new ParseResultInteger(0);
		}

		@Override
		protected ParseResult addResult(ParseResult resultAdd) 
		{
			m_scValue = m_scValue + resultAdd.toString();
			return this;
		}

		@Override
		protected ParseResult subResult(ParseResult resultSub) 
		{
			// Cannot sub reference so leave it as is
			return this;
		}

		@Override
		protected ParseResult multiplyResult(ParseResult resultMultiply) 
		{
			// Cannot use reference so leave it as is
			return this;
		}

		@Override
		protected ParseResult divideResult(ParseResult resultDivide) 
		{
			// Cannot use reference so leave it as is
			return this;
		}				
	}
	
	class ParseReference
	{
		public ParseReference()
		{
		}
		
		private CategoryData m_dataCategory = null;
		private SelectionData m_dataSelection = null;
		private Object m_oValue = null;
		
		protected void element(CharacterData dataCharacter, String scSource) throws EvaluationHelperException
		{
			if (null==m_dataCategory)
			{
				m_dataCategory = dataCharacter.getCategory(scSource);
				return;
			}
			
			if (null==m_dataSelection)
			{
				m_dataSelection = (SelectionData)m_dataCategory.getRegion(scSource);
			}
			
			if (null==m_dataSelection)
			{
				throw new EvaluationHelperException("Could not find " + scSource);
			}
			
			if (scSource.startsWith("select"))
			{
				if (m_dataSelection.isSelected())
					m_oValue = new ParseResultInteger(1);
				else
					m_oValue = new ParseResultInteger(0);
				return;
			}
			
			return;
		}
		
		public ParseResult getResult()
		{
			if (null==m_dataSelection)
			{
				return new ParseResultString("Error with reference ");
			}
			
			if (null==m_oValue) m_oValue = m_dataSelection.getValue();
			
			if (m_oValue instanceof ParseResult)
			{
				// The result return is used in evaluation so need to copy it otherwise will change result of another selection!
				ParseResult resultValue = (ParseResult)m_oValue;
				return resultValue.copyResult();
			}
			
			return new ParseResultString(m_oValue.toString());
		}
		
		public SelectionData getReference()
		{
			return m_dataSelection;
		}

		public void reset() 
		{
			m_dataCategory = null;
			m_dataSelection = null;
			m_oValue = null;
		}		
	}
	
	public ParseResult evaluateObject(SelectionData dataSelection, ValueDataMath dataValue)
	{
		ParseResult resultEvaluation = evaluateObject(dataSelection, dataValue.getExpression());

		ValueData dataPrevious = dataValue.getNextValue();		

		if (dataPrevious instanceof ValueDataMath) // null is false
		{
			dataValue = (ValueDataMath)dataPrevious;
			resultEvaluation = evaluateChain(dataSelection, resultEvaluation, dataValue);
		}
		
		return resultEvaluation;
	}
	
	private ParseResult evaluateChain(SelectionData dataSelection, ParseResult resultEvaluation, ValueDataMath dataPrevious)
	{
		try
		{
			ParseResult resultPrevious = evaluateObject(dataSelection, dataPrevious);
			return resultEvaluation.add(resultPrevious);
		}
		catch (EvaluationHelperException x)
		{
			return new ParseResultString(x.getMessage());
		}
	}
	
	public ParseResult evaluateObject(SelectionData dataSelection, String scExpression)
	{
		try
		{
			ParseArgument evaluateArgument = new ParseArgument(scExpression);
			return evaluateThirteenthOperators(evaluateArgument);		
		}
		catch (EvaluationHelperException x)
		{
			return new ParseResultString(x.getMessage());
		}
	}
	
	// Operator precedence
	// 1 ()
	// 2 Unary
	// 3 * /
	// 4 + -
	// 6 < <= > >=
	// 7 = ! {used in this case as binary 'not equal'}
	// 13 ?
	
	private ParseResult evaluateThirteenthOperators(ParseArgument argument) throws EvaluationHelperException
	{
		ParseResult resultBoolean = evaluateSixthSeventhOperators(argument);
		
		while (argument.hasSymbol())
		{
			int iCharacter =  argument.nextChar();
			
			if ('?'==iCharacter) 
			{
				ParseResult resultTest = evaluateSixthSeventhOperators(argument.consume(1));
				resultBoolean = resultBoolean.compareTrue(resultTest);
				continue;
			}

			break;
		}
		
		return resultBoolean;
	}
	
	private ParseResult evaluateSixthSeventhOperators(ParseArgument argument) throws EvaluationHelperException
	{
		ParseResult resultCompare = evaluateFourthOperators(argument);
		
		while (argument.hasSymbol())
		{
			int iCharacter =  argument.nextChar();
			
			if ('>'==iCharacter) 
			{
				resultCompare = evaluatePairOperators(resultCompare, iCharacter, argument.consume(1));
				continue;
			}

			if ('<'==iCharacter) 
			{
				resultCompare = evaluatePairOperators(resultCompare, iCharacter, argument.consume(1));
				continue;
			}
			
			if ('='==iCharacter) 
			{
				ParseResult resultEQ = evaluateFourthOperators(argument.consume(1));
				resultCompare = resultCompare.compareEQ(resultEQ);
				continue;
			}
			
			if ('!'==iCharacter) 
			{
				ParseResult resultNE = evaluateFourthOperators(argument.consume(1));
				resultCompare = resultCompare.compareNE(resultNE);
				continue;
			}
			
			break;
		}
		
		return resultCompare;
	}
	
	private ParseResult evaluatePairOperators(ParseResult result, int iFirst, ParseArgument argument) throws EvaluationHelperException
	{
		int iCharacter =  argument.nextChar();

		if ('>'==iFirst && '='==iCharacter)
		{
			ParseResult resultGE = evaluateFourthOperators(argument.consume(1));
			return result.compareGE(resultGE);
		}
		
		if ('<'==iFirst && '='==iCharacter)
		{
			ParseResult resultLE = evaluateFourthOperators(argument.consume(1));
			return result.compareLE(resultLE);
		}
		
		if ('>'==iFirst)
		{
			ParseResult resultGT = evaluateFourthOperators(argument);
			return result.compareGT(resultGT);
		}
		
		if ('<'==iFirst)
		{
			ParseResult resultLT = evaluateFourthOperators(argument);
			return result.compareLT(resultLT);
		}
		
		return result;
	}
	
	private ParseResult evaluateFourthOperators(ParseArgument argument) throws EvaluationHelperException
	{
		ParseResult result = evaluateThirdOperators(argument);
		
		while (argument.hasSymbol())
		{
			int iCharacter =  argument.nextChar();
			if ('+'==iCharacter) 
			{
				ParseResult resultAdd = evaluateThirdOperators(argument.consume(1));
				result = result.add(resultAdd);
				continue;
			}
			if ('-'==iCharacter) 
			{
				ParseResult resultSub = evaluateThirdOperators(argument.consume(1));
				result = result.sub(resultSub);
				continue;
			}
			break;
		}
		
		return result;
	}
	
	private ParseResult evaluateThirdOperators(ParseArgument argument) throws EvaluationHelperException
	{
		ParseResult result = evaluateFirstSecondOperators(argument);
		
		while (argument.hasSymbol())
		{
			int iCharacter = argument.nextChar();
			if ('*'==iCharacter) 
			{
				ParseResult resultProduct = evaluateFirstSecondOperators(argument.consume(1));
				result = result.multiply(resultProduct);
				continue;
			}
			if ('('==iCharacter) 
			{
				ParseResult resultProduct = evaluateFirstSecondOperators(argument);
				result = result.multiply(resultProduct);
				continue;
			}
			if ('/'==iCharacter) 
			{
				ParseResult resultDivide = evaluateFirstSecondOperators(argument.consume(1));
				result = result.divide(resultDivide);
				continue;
			}
			break;
		}
		
		return result;
	}
	
	private ParseResult evaluateFirstSecondOperators(ParseArgument argument) throws EvaluationHelperException
	{
		ParseResult result = null;
		
		int iCharacter =  argument.nextChar();
		
		if ('-'==iCharacter)
		{
			result = evaluateFirstSecondOperators(argument.consume(1));
			return result.multiply(new ParseResultInteger(-1));
		}
		
		if ('+'==iCharacter)
		{
			return evaluateFirstSecondOperators(argument.consume(1));
		}
		
		if ('('==iCharacter)
		{
			result = evaluateThirteenthOperators(argument.consume(1));
			iCharacter =  argument.nextChar();
			if (')'!=iCharacter) throw new RuntimeException("Invalid formula - unbalanced '(' ')'");
			argument.consume(1);
			return result;
		}

		return evaluateSymbol(argument);		
	}
	
	private ParseResult evaluateSymbol(ParseArgument argument) throws EvaluationHelperException
	{
		int iCharacter =  argument.nextChar();
		
		if (Character.isDigit(iCharacter))
		{
			String scValue = argument.getNumber();
			
			if (argument.isDecimal())
			{
				double dValue = Double.parseDouble(scValue);
				return new ParseResultDouble(dValue);
			}
			
			if (argument.isCurrency())
			{
				int iValue = m_dataCharacter.getCurrencyValueBase(scValue);
				return new ParseResultInteger(iValue);
			}
			
			int iValue = Integer.parseInt(scValue);
			return new ParseResultInteger(iValue);
		}
		
		ParseReference resultReference = referenceSymbol(argument);
		
		return resultReference.getResult();
	}
	
	protected ParseReference referenceSymbol(ParseArgument argument) throws EvaluationHelperException
	{
		ParseReference resultReference = new ParseReference();
		
		while (argument.hasSymbol())
		{
			String scSource = argument.getStringRegex();
			if (null==scSource) break;
			resultReference.element(m_dataCharacter, scSource);
			
			int iCharacter =  argument.nextChar();			
			if ('.'!=iCharacter) break;
		}
		
		return resultReference;
	}
	
	public SelectionData referenceSelection(String scSymbol)
	{
		ParseArgument argument = new ParseArgument(scSymbol);
		
		try 
		{
			ParseReference result = referenceSymbol(argument);
			return result.getReference();
		} 
		catch (EvaluationHelperException x)
		{
			return new SelectionData(x.getMessage());
		}
	}
	
	class EvaluationHelperException extends Exception
	{
		private static final long serialVersionUID = 4853431392065079566L;

		private EvaluationHelperException(String scException)
		{
			super(scException);
		}
	}
}

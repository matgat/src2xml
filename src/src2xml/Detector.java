/**
 *  This unit contains the abstract class
 *  Detector and its specializations,
 *  suitable to identify a particular
 *  occurrence in source file.
 *
 *  @author Matteo Gattanini
 *  @see ElementReader
 */

package src2xml;

// Containers
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
// IO
import java.io.IOException;
// Regex
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/////////////////////////////////////////////////////////////////
// Detectors
/////////////////////////////////////////////////////////////////

//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  A Detector is an object suitable to identify a
 *  particular occurrence in source file.
 *  This is an abstract interface to generic occurrence
 *  detection in the source stream.
 *
 */
public abstract class Detector
{
    /**
     *  Class constructor
     *  @param   inc   A boolean that states if include or
     *                 not detected occurrence
     */
    public Detector(boolean inc) {include = inc;}

 // . . . Main interface
    /**
     *  Read an element from source
     *  @param  src          The source stream
     *  @return              Return true if an occurrence is detected
     *  @throws IOException  If an input exception occurred
     */
    abstract public boolean IsOccurred(Source src) throws IOException;
    // . . . Attributes
   /**
    *  Occurrence string
    */
    public String occurrence = "";
   /**
    *  Include or exclude triggering pattern
    */
    public boolean include = false; 
}


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Dummy detector always true or false
 *
 */
class FixedDetector extends Detector
{
 // . . . Constructor
    public FixedDetector(boolean res)
    {
        super(false);
        result = res;
    }

 // . . . Main interface
    public boolean IsOccurred(Source src) throws IOException { return result; }
    @Override public String toString() {return Boolean.toString(result);}

 // . . . Attributes
    private boolean result = false;
} // end 'FixedDetector'


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Detects whether source has ended
 *
 */
class SourceEndDetector extends Detector
{
 // . . . Constructor
    public SourceEndDetector() { super(false); }

 // . . . Main interface
    public boolean IsOccurred(Source src) throws IOException { return src.Ended(); }
    @Override public String toString() {return "Source end detector";}
} // end 'SourceEndDetector'


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Fixed String detector
 */
class StringDetector extends Detector
{
  /**
   *  Constructor
   *  @param   ptn       String pattern
   *  @param   nocase    Case insensitiveness
   *  @param   inc       Detector occurrence inclusion
   *  throws Exception   If something goes wrong
   */
    public StringDetector(String ptn, boolean nocase, boolean inc)
    {
        super(inc);
        pattern = ptn;
        case_insensitive = nocase;
        length = pattern.length();
    }

 // . . . Main interface
    public boolean IsOccurred(Source src) throws IOException
    {
     src.setMark();
     occurrence = src.read( length );

     boolean matches = case_insensitive ? pattern.equalsIgnoreCase(occurrence) : pattern.equals(occurrence);
     if ( matches )
          {
           if (include) src.releaseMark();
           else src.restoreMark();
           return true;
          }
     else {
           src.restoreMark();
           return false;
          }
    }
    @Override public String toString() {return pattern;}

 // . . . Attributes
    private String pattern;
    private boolean case_insensitive = false; // An option
    private int length;
} // end 'StringDetector'


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Detect a String belonging to a String group
 */
class KeywordDetector extends Detector
{
  /**
   *  Constructor
   *  @param   kws       Keywords spaced strings like: "int char double"
   *  @param   nocase    Case insensitiveness
   *  @param   inc       Detector occurrence inclusion
   *  throws Exception   If something goes wrong
   */
    public KeywordDetector(String kws, boolean nocase, boolean inc) throws Exception
    {
        super(inc);
        set_keywords(kws);
        case_insensitive = nocase;
        end = new CharDetector(new CharGroupNot("a…z A…Z _ 0…9"), false);
    }

 // . . . Main interface
    public boolean IsOccurred(Source src) throws IOException
    {
        char c1,c2;
        occurrence = "";
        src.setMark(); // Restore status if not triggered

        // Get first-character-matching keywords
        List<String> matching_keys = new LinkedList<String>();
        for (String k : keywords) if (k.charAt(0) == src.currChar()) matching_keys.add(k);

        // Now see following characters
        String matching = ""; // bag for matching keyword
        int t = 0; // test counter
        while ( src.nextChar() )
            {
             if ( end.IsOccurred(src) ) break;
             // Any previous matching is now invalid
             matching = "";
             if ( matching_keys.isEmpty() || (++t >= keywords_maxlength) ) break;

             // 'c' is a character to check
             for ( Iterator<String> k=matching_keys.iterator(); k.hasNext(); ) // for (String k : matching_keys)
                 {
                  String kw = k.next();
                  if ( t < kw.length() )
                       {
                        c1 = kw.charAt(t);
                        c2 = src.currChar();
                        if ( case_insensitive )
                           {
                            c1 = Character.toLowerCase(c1);
                            c2 = Character.toLowerCase(c2);
                           }
                        if ( c1 == c2 )
                             {
                              // If it was the last char, store and remove
                              if ( kw.length() == t+1 )
                                 {
                                  matching = kw;
                                  k.remove();
                                 }
                             }
                        else k.remove(); // Remove unmatching
                       }
                 } // end for each partial-matching keyword
            } // end 'while there is source'

        // Check if found something
        if ( matching.isEmpty() )
             {
              src.restoreMark();
              return false;
             }
        else {
              src.releaseMark();
              occurrence = matching;
              return true;
             }
    }
    //@Override public String toString() {return occurrence;}
    public void assignEndDetector(Detector d) {end = d;}

    // . . . Other
    // Get keywords from spaced strings like: "int char double"
    private void set_keywords(String s)
    {
        String[] kws = s.split("\\s+"); // one or more whitespace characters
        keywords_maxlength = 0;
        keywords_minlength = Integer.MAX_VALUE;
        for (int i=0; i<kws.length; ++i)
            {
             String kw = kws[i].trim();
             if ( !kw.isEmpty() )
                {
                 keywords.add(kw);
                 if ( keywords_maxlength < kw.length() ) keywords_maxlength = kw.length();
                 if ( keywords_minlength > kw.length() ) keywords_minlength = kw.length();
                }
            }
    }

 // . . . Attributes
    private Set<String> keywords = new HashSet<String>();
    private Detector end; // when finish to read a keyword
    private boolean case_insensitive = false; // An option
    private int keywords_maxlength = 0;
    private int keywords_minlength = Integer.MAX_VALUE;
} // end 'KeywordDetector'


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Regular expression detector
 *
 */
class RegexDetector extends Detector
{
 // . . . Constructor
    public RegexDetector(String regex, boolean inc)
    {
        super(inc);
        pattern = Pattern.compile(regex);
    }

 // . . . Main interface
    public boolean IsOccurred(Source src) throws IOException
    {
        occurrence = "";
        Matcher m = pattern.matcher(src.asCharSequence());

        if ( m.find(src.currCharIdx()) )
             {
              if ( m.start() == src.currCharIdx() )
                 {
                  // Yes, here there is a matching pattern
                  occurrence = m.group();
                  if (include) src.read( occurrence.length() ); // eat
                  return true;
                 }
             }
        return false;
    }
    @Override public String toString() {return pattern.toString();}

 // . . . Attributes
    private Pattern pattern = null;
} // end 'RegexDetector'


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Dummy Detector that takes a fixed number of characters
 *
 */
class CharNumDetector extends Detector
{
 // . . . Constructor
    public CharNumDetector(int chnum)
    {
        super(true);
        char_num = chnum;
    }

 // . . . Main interface
    public boolean IsOccurred(Source src) throws IOException
    {
        if(!include) src.setMark();
        occurrence = src.read( char_num );
        if(!include) src.restoreMark();
        return true;
    }
    @Override public String toString() {return Integer.toString(char_num);}

 // . . . Attributes
    private int char_num = 0;
} // end 'CharNumDetector'



//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Fixed character Detector
 *
 */
class CharDetector extends Detector
{
 // . . . Constructor
    public CharDetector(CharGroup cs, boolean inc)
    {
        super(inc);
        char_group = cs;
    }

 // . . . Main interface
    public boolean IsOccurred(Source src) throws IOException
    {
     occurrence = "";
     if ( char_group.Contains( src.currChar() ) )
         {
          occurrence = Character.toString(src.currChar());
          if (include) src.nextChar(); // eat char
          return true;
         }
     else return false;
    }
    @Override public String toString() {return char_group.toString();}

 // . . . Attributes
    private CharGroup char_group = null;
} // end 'CharDetector'



/////////////////////////////////////////////////////////////////
// Character utilities
/////////////////////////////////////////////////////////////////


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  A character set (composed by ranges and single chars)
 *  Interface to character range or single
 *
 */
class CharGroup
{
 // . . . Constructor
    public CharGroup(String s) throws Exception
    {
        // Get the character blocks from strings like "a A…Z # b…\t \'…v \u0075…\u0077"
        String[] blks = s.split("\\s+");
        for (int i=0; i<blks.length; ++i)
            {
             // Ignore empty strings
             if ( (blks[i]=blks[i].trim()).isEmpty() ) continue;
             // Translate possibly escaped characters
             blks[i] = StringUtils.ProcessEscapeSeqs(blks[i]);
             // Char blocks may be of type 'A' (single character) or 'a-z' (range)
             switch ( blks[i].length() )
                {
                 case 1 : charblocks.add(new CharSingle(blks[i].charAt(0))); break;
                 case 3 : charblocks.add(new CharRange(blks[i].charAt(0), blks[i].charAt(2))); break;
                 default: throw new Exception("(!) Invalid character block: " + blks[i]);
                } // end switch block length
            }
    }
    public CharGroup(String s, String sid) throws Exception { this(s); id = sid; }

 // . . . Main interface
    public boolean Contains(char c)
    {
        for (CharBlock chb : charblocks) { if(chb.Match(c)) return true; }
        return false;
    }
    public String getId() { return id; }
    @Override public String toString() {String s=""; for (CharBlock chb : charblocks) s+=chb+" "; return s.trim();}

 // . . . Attributes
    private Collection<CharBlock> charblocks = new ArrayList<CharBlock>();
    private String id = "";
}
// A negated version (chars not belonging to group)
class CharGroupNot extends CharGroup
{
    public CharGroupNot(String s) throws Exception {super(s);}
    public CharGroupNot(String s, String sid) throws Exception {super(s,sid);}
     // . . . Main interface
    @Override public boolean Contains(char c) {return !super.Contains(c);}
}


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Interface to generic character comparison unit
 *
 */
interface CharBlock
{
    public boolean Match(char c);
}


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  A character range
 *
 */
class CharRange implements CharBlock
{
 // . . . Constructor
    public CharRange(char min, char max){cmin=min; cmax=max;}
 // . . . Main interface
    public boolean Match(char c) {return (c>=cmin && c<=cmax);}
    @Override public String toString() {return (cmin+"…"+cmax);}
 // . . . Attributes
    private char cmin = '\0';
    private char cmax = '\0';
}

//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  A single character
 *
 */
class CharSingle implements CharBlock
{
 // . . . Constructor
    public CharSingle(char c){ch=c;}
 // . . . Main interface
    public boolean Match(char c) {return (c == ch);}
    @Override public String toString() {return Character.toString(ch);}
 // . . . Attributes
    private char ch = '\0';
}

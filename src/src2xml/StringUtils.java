package src2xml;

// Containers
import java.util.Map;
import java.util.HashMap;
// Regex
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/////////////////////////////////////////////////////////////////////////////
/**
 *  This class collects some String facilities
 *  @author Matteo Gattanini
 *
 */
public class StringUtils
{
 // . . . Attributes
 static private MultipleReplacer single_escseqs_replacer = new MultipleReplacer("(\\\\,\\) (\\t,\t) (\\n,\n) (\\r,\r) (\\f,\f) (\\b,\b) (\\\',\') (\\\",\")");

 // . . . Methods

    //-----------------------------------------------------------
    /**
     *  Translates the possibly escape sequences.
     *  Incredible! Java does not provide one?
     *  @param  s            The input String
     *  @return              The String containing the escaped characters
     *  @throws Exception    If something wrong with Regex stuff
     *  @see                 java.lang.String
     *  @see                 java.util.regex.Pattern
     */
    public static String ProcessEscapeSeqs(String s) throws Exception
    {
        // Deal with simple escape sequences
        s = single_escseqs_replacer.doReplace(s);

        // Deal with complex patterns uhhhh,xhh,ooo
        Matcher m = Pattern.compile("\\\\u([0-9a-fA-F]{4})|\\\\x([0-9a-fA-F]{2})|\\\\([0-7]{3})").matcher(s);

        // Here we go...
        StringBuffer sb = new StringBuffer(s.length());
        while ( m.find() )
           {
            String esc = m.group();
            Character c = (char) ( Character.isDigit(esc.charAt(1)) ?
                                   Integer.parseInt(esc.substring(1), 8) : // octal
                                   Integer.parseInt(esc.substring(2), 16) );// hexadecimal
            m.appendReplacement(sb, c.toString());
           }
       m.appendTail(sb);
       return sb.toString();
    } // end 'ProcessEscapeSeqs'


    //-----------------------------------------------------------
    /**
     *  Translates a glob pattern to a regex string
     *  @param  line         The input glob pattern such as "ar*.c?p"
     *  @return              The regexp String
     *  @see                 java.lang.String
     *  @see                 java.util.regex.Pattern
     */
 public static String Glob2RegEx(String line)
    {
     line = line.trim();
     int len = line.length();
     StringBuilder sb = new StringBuilder(len);
     // Remove beginning and ending * globs because they're useless
     if(len>1 && line.startsWith("*")){ line = line.substring(1); --len; }
     if(len>1 && line.endsWith("*")) line = line.substring(0, --len);
     boolean escaping = false;
     int inCurlies = 0;
     for(int i=0; i<len; ++i)
        {
         switch (line.charAt(i))
           {
            case '*':
                if(escaping) sb.append("\\*");
                else sb.append(".*");
                escaping = false;
                break;
            case '?':
                if(escaping) sb.append("\\?");
                else sb.append('.');
                escaping = false;
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                sb.append('\\');
                sb.append(line.charAt(i));
                escaping = false;
                break;
            case '\\':
                if (escaping) {sb.append("\\\\"); escaping=false;}
                else escaping = true;
                break;
            case '{':
                if(escaping) sb.append("\\{");
                else { sb.append('('); ++inCurlies; }
                escaping = false;
                break;
            case '}':
                if (inCurlies > 0 && !escaping) {sb.append(')'); --inCurlies;}
                else if (escaping) sb.append("\\}");
                else sb.append("}");
                escaping = false;
                break;
            case ',':
                if (inCurlies>0 && !escaping) sb.append('|');
                else if(escaping) sb.append("\\,");
                else sb.append(",");
                break;
            default:
                escaping = false;
                sb.append(line.charAt(i));
           }
        }
     return sb.toString();
} // end 'Glob2RegEx'



 // . . . Classes

    /////////////////////////////////////////////////////////////////////////
    /**
     *  A class that performs multiple replacement in a given String
     *
     */
    static public class MultipleReplacer
    {
        /**
         *  Class constructor
         *  @param   s_pairs   Initialize replacing pairs
         */
        public MultipleReplacer(String s_pairs)
        {
            try { addPairs(s_pairs); }
            catch(Exception e) {System.err.println(e.getMessage());}
        }
        public MultipleReplacer() {;}

     // . . . Main interface
        /**
         *  Retrieve replacement pairs from a String.
         *  Known bugs: problems may arise when strings contain
         *  regex special characters
         *  @param  s_pairs        The input String.
         *                         It must have the format: {@code "(old1,new1) (old2,new2) (old3,new3) ..."}
         *  @throws  Exception     If the input String is invalid
         */
        public void addPairs( String s_pairs ) throws Exception
        {
            //String[] p = s.split("(^|)\\s*(|)$"); for(int i=0; i<p.length; ++i)...
            Matcher matcher = Pattern.compile("\\((.*?)\\)").matcher(s_pairs);
            while ( matcher.find() )
               {
                String[] s_pair = matcher.group(1).split(",");
                if ( s_pair.length != 2 ) throw new Exception("Invalid replacement pair in " + matcher.group());
                else addPair( s_pair[0], s_pair[1] );
               }
        }

        /**
         *  Add a replacement pair (old string, new string)
         *  Be sure to not add multiple times the same String
         *  to replace
         *  @param  old_s     The occurrences to be replaced
         *  @param  new_s     The replacing String
         */
        public void addPair( String old_s, String new_s )
        {
            pairs.put(old_s, new_s);
        }

        /**
         *  Apply the collected replacement pairs
         *  @param  s            The input String to process
         *  @return              The String after replacements
         *  @throws Exception    If something goes wrong
         */
        public String doReplace( String s ) throws Exception
        {
            // Do nothing if no replacement pairs
            if ( pairs.isEmpty() ) return s;

            // Build the regular expression
            Pattern ptrn_regexspchars = Pattern.compile("[\\\\\\^\\$\\.\\|\\?\\*\\+\\(\\)\\{\\}\\[\\]]");
            StringBuilder regex = new StringBuilder("(");
            for (String k : pairs.keySet())
               {
                // Must fix regex special characters: ehm, cannot use myself for this!
                k = ptrn_regexspchars.matcher(k).replaceAll("\\\\$0");
                regex.append(k+"|");
               }
            regex.setCharAt(regex.length()-1, ')');
            //System.out.println(regex); // D_EBUG

            // Instantiate the required objects
            Matcher matcher = Pattern.compile(regex.toString()).matcher(s);
            StringBuffer sb = new StringBuffer(s.length());

            // Here we go...
            while ( matcher.find() )
               {
                String replacing = pairs.get(matcher.group());
                if ( replacing == null ) throw new Exception("Ehmm, check string pairs in MultipleReplacer, cannot find: " + matcher.group());
                matcher.appendReplacement(sb, replacing);
               }
            matcher.appendTail(sb);

            return sb.toString();
        }

     // . . . Attributes
        private Map<String,String> pairs = new HashMap<String,String>(); // Replacement pairs
    } // end class 'MultipleReplacer'

} // end class 'StringUtils'

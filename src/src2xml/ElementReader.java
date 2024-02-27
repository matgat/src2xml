package src2xml;

// Containers
import java.util.Collection;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
// IO
import java.io.IOException;


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  ElementReader is a class that represents
 *  the generic reader of a source region (element),
 *  that is what identifies and stores a particular
 *  region in the source code.<p>
 *  It uses two Detectors to recognize the start and
 *  the end of the region.
 *
 *  @author Matteo Gattanini
 *  @see Detector
 */
public class ElementReader
{
    /**
     *  Class constructor
     *  @param   sid     An IDentificative String for this reader
     *  @param   st      The element (region) start Detector
     *  @param   en      The element (region) end Detector
     *  @param   sptn    The exported region starting tag pattern - use var (id)
     *  @param   eptn    The exported region ending tag pattern - use var (id)
     *  @param   nes     A boolean true if read elements can appear only as
     *                   nested in other elements
     *  @see             Detector
     */
    public ElementReader(String sid, Detector st, Detector en, String sptn, String eptn, boolean nes)
    {
        id = sid; // assign the id String
        start = st;
        end = en;
        // Exporter settings
        start_pattern = sptn;
        end_pattern = eptn;
        nested_only = nes;
    }

 // . . . Main interface
    /**
     *  Read an element from source
     *  @param  src          The source stream
     *  @return              The read Element object
     *  @throws IOException  If an input or output
     *                       exception occurred
     *  @see           Source
     *  @see           Element
     */
    public Element Read(Source src) throws IOException
    {
        // If here an element is triggered!
        ElementNode curr_elem = new ElementNode(id, start_pattern, end_pattern);

        //System.out.println("> Element reader " + id + " reads beginning from char: " +src.currChar() + "(0x" + Integer.toHexString(src.currChar()) + "): " + content); // D EBUG

        while ( !(src.Ended() || Ends(src)) )
        {
            // Check for nested regions
            boolean subelement_encountered = false;
            for (ElementReader elreader : nested)
            {
                if ( elreader.Triggers(src) )
                {
                    subelement_encountered = true;

                    // Flush possibile preceding content
                    if (!content.isEmpty()) curr_elem.addChild( new ElementLeaf(content) );
                    content = "";

                    // Get sub-elements recursively
                    curr_elem.addChild( elreader.Read(src) );
                    break; // exit sub-elements check
                }
            } // end 'for each nested regions'

            //char nextchar = (char) src.read(); // proceed
            if ( !subelement_encountered )
               {
                content += src.currChar();
                src.nextChar();
               }
        } // end 'until end detector or source ended'

        // Flush possibile content
        if (!content.isEmpty()) curr_elem.addChild( new ElementLeaf(content) );
        content = "";

        return curr_elem;
    }

    /**
     *  Tells if this region starts at current,
     *  position in source, invoking the corresponding
     *  Detector
     *  @param   src         The source stream
     *  @return              The boolean result
     *  @throws IOException  If an input or output exception occurred
     *  @see           Source
     *  @see           Detector
     */
    public boolean Triggers(Source src) throws IOException
    {
        if ( start.IsOccurred(src) )
           {
            if(start.include) content = start.occurrence;
            //System.out.println("> Reader " + id + " triggers with: " + start.occurrence); // D EBUG
            return true;
           }
        else return false;
    }

    /**
     *  Detects the end of read region, invoking
     *  the corresponding Detector
     *  @param   src         The source stream
     *  @return              The boolean result
     *  @throws IOException  If an input or output exception occurred
     *  @see           Source
     *  @see           Detector
     */
    public boolean Ends(Source src) throws IOException
    {
        if ( end.IsOccurred(src) )
           {
            //System.out.println("> Reader " + id + " ends with: " + end.occurrence); // D EBUG
            if(end.include) content += end.occurrence;
            return true;
           }
        else return false;
    }

 // . . . Other
    /**
     *  Add a new nested region, that is an ElementReader
     *  that might be contained in this one
     *  @param   e   An ElementReader expected to be nested
     */
    public void AddNested(ElementReader e) {if(e!=null) nested.add(e);}

    /**
     *  Generate an estimated CSS based on child IDs
     *  @return   A String containing CSS code
     */
    public String generateCSS()
    {
        // Preparation
        String indent = "     ";
        StringBuilder sb = new StringBuilder("/* Use this stylesheet as a starting template */" + Main.eol);

        // Just one little thing...
        String color = id.contains("html") ||
                       id.contains("xml")  ||
                       id.contains("tex")  ?  "Black" : "Teal";
        // Root entry
        sb.append("." + id + Main.eol);
        sb.append("    {" + Main.eol);
        sb.append(indent + "background-color: Snow;" + Main.eol);
        sb.append(indent + "border: thick solid LightSteelBlue;" + Main.eol);
        sb.append(indent + "white-space: pre;" + Main.eol);
        sb.append(indent + "padding: 1ex;" + Main.eol);
        sb.append(indent + "font-family: Consolas,\'Courier New\',monospace;" + Main.eol);
        sb.append(indent + "color: " + color + "; /* default font color */" + Main.eol);
        sb.append("    }" + Main.eol + Main.eol);

        // Cycle all childs to include also nested-only regions
        Set<String> done_ids = new HashSet<String>();
        done_ids.add(id); // No more root entry
        getCSSentry (sb, indent, done_ids);

        return sb.toString();
    }
    /**
     *  An auxiliary function used by 'generateCSS'
     *  @param    sb        The filled string buffer
     *  @param    indent    Indenting spaces
     *  @param    done_ids  Already considered region ids
     */
    private void getCSSentry (StringBuilder sb, String indent, Set<String> done_ids)
    {
        // Check if it has been done yet or not
        if ( !done_ids.contains(id) )
           {
            done_ids.add(id);

            if ( !id.isEmpty() )
               {
                // Decide some attributes (at least try to do so)
                String color = "color: Black;";
                String style = "";
                if ( id.contains("comment") )
                   {
                    color = "color: DarkSeaGreen; background-color: FloralWhite;";
                    style = "font-style: italic;";
                   }
                else if ( id.contains("dir") || id.contains("annot") || id.contains("spec") || id.contains("proc") )
                   {
                    color = "color: MediumSlateBlue;";
                    style = "background-color: HoneyDew;";
                   }
                else if ( id.contains("ext") || id.equalsIgnoreCase("css-id") )
                   {
                    color = "color: DarkBlue;";
                    style = "font-weight: bold;";
                   }
                else if ( id.contains("key") )
                   {
                    style = "font-weight: bold;";
                   }
                else if ( id.contains("num") || id.contains("attrib") || id.contains("var"))
                   {
                    color = "color: DodgerBlue;";
                   }
                else if ( id.contains("esc") || id.contains("entity") || id.contains("val") )
                   {
                    color = "background-color: AntiqueWhite;";
                    style = "border: 1px solid Wheat;";
                   }
                else if ( id.contains("quot") || id.contains("arg") )
                   {
                    color = "color: DarkGoldenRod;";
                   }
                else if ( id.contains("element") || id.contains("namespace") )
                   {
                    color = "color: RoyalBlue;";
                   }
                else if ( id.contains("macro") || id.contains("class") )
                   {
                    color = "color: RoyalBlue;";
                    style = "background-color: AntiqueWhite;";
                   }
                else if ( id.contains("label") )
                   {
                    color = "color: FireBrick; background-color: LightGoldenRodYellow;";
                    style = "font-weight: bold;";
                   }
                else if ( id.contains("script") || id.contains("style") )
                   {
                    style = "background-color: MintCream;";
                   }
                else if ( id.contains("data") )
                   {
                    style = "background-color: Lavender;";
                   }

                 // Now write something
                sb.append("." + id + Main.eol);
                sb.append("    {" + Main.eol);
                if ( !color.isEmpty() ) sb.append(indent + color + Main.eol);
                if ( !style.isEmpty() ) sb.append(indent + style + Main.eol);
                sb.append("    }" + Main.eol + Main.eol);
               } // end 'id not empty'
           } // end 'id not yet done'
        // Call recursively
        for ( ElementReader e : nested )
        {
            if ( !done_ids.contains(e.id) ) e.getCSSentry(sb, indent, done_ids);
        }
    }

 // . . . Attributes
    public String id = ""; // Identificative string
    private Collection<ElementReader> nested = new ArrayList<ElementReader>(); // nested elements
    protected String content = ""; // A bag for plain text
    private Detector start, end;
    // Config facilities
    public String nested_bag = ""; // A bag to store nested elements list
    public boolean nested_only = false; // A bag to store if must be added or not to root reader
    // Exporter settings
    public String start_pattern = ""; // Can contain var (id)
    public String end_pattern = "";
} // end 'ElementReader'

package src2xml;

// Containers
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
// IO
import java.io.File;
import java.io.InputStream;
import java.io.FileReader;
// XML reader
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.*;
import javax.xml.namespace.QName;


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  ConfigurationGetter is a class whose duty is
 *  to retrieve the configuration from a XML file,
 *  filling the readers tree from it.
 *
 *  @author Matteo Gattanini
 *  @see ElementReader
 */
class ConfigurationGetter
{
    /**
     *  Class constructor
     *  @param  cfile        xml config file
     *  @param  all          xml config file
     *  @throws Exception  If something wrong during restoration
     *
     */
    public ConfigurationGetter(File cfile) throws Exception
    {
        assert (cfile != null ) : "Passed a null file!";
        // Restore config file if not existing
        config_file = cfile;
        if ( !config_file.exists() )
           {
            // Restore config file
            System.out.println("Restoring config file " + config_file.getPath());
            OutputWriter w = new OutputWriter(config_file);
            InputStream stream = getClass().getResourceAsStream(Main.resPath);
            if (stream != null) w.Write(stream);
            else throw new Exception("Resource not found, cannot restore config file!");
           }
       // Load langs
       getReaders(null); // Specify an extension if you want to load just a lang
    } // end 'constructor'


    /**
     *  Essentially fills the element readers tree
     *  from the XML config file
     *  @param ext         Specific lang extension, use 'null' to load all langs
     *  @throws Exception  Typically if config file is bad
     *  @see               ElementReader
     */
    public void getReaders(String ext) throws Exception
    {
        boolean all_langs = (ext==null);
        // Retrieved
        langs = new LinkedHashMap<String,Map<String,ElementReader>>(); // Language readers map
        chargroups = new HashMap<String,CharGroup>(); // Defined charsets
        extaliases = new HashMap<String,String>(); // Defined extensions aliases

        // . . . Parse xml config file
        System.out.println("Retrieving configuration from " + config_file.getPath());
        status = Status.START;
        XMLEventReader xml_reader = XMLInputFactory.newInstance().createXMLEventReader(new FileReader(config_file));
        try{
            while ( xml_reader.hasNext() && status!=Status.ALL_DONE )
              {
               XMLEvent event = xml_reader.nextEvent(); // nextTag();
               //System.out.println("New XML event>\t" + event.toString()); // D EBUG

               switch (status)
                  {
                   case START :
                       // Well, must wait to enter in 'TAG_COMMON'
                       //if ( isStartTag(event,TAG_ROOT) ) status = Status.START;
                       if ( isStartTag(event,TAG_COMMON) ) status = Status.GET_COMMON;
                       break;

                   case GET_COMMON :
                       // Well, for now just 'TAG_CHARGROUP' is expected
                       if ( isStartTag(event,TAG_CHARGROUP) )
                          {
                           CharGroup cg = getCharGroup(xml_reader,event.asStartElement(),chargroups);
                           chargroups.put(cg.getId(), cg);
                          }
                       else if ( isStartTag(event,TAG_EXPORTER) )
                          {
                           // Get default exported tag and attribute
                           exporter_root_start = getAttribute(event.asStartElement(), ATT_ROOT_STARTTAG);
                           exporter_root_end = getAttribute(event.asStartElement(), ATT_ROOT_ENDTAG);
                           exporter_root_id_suffix = getAttribute(event.asStartElement(), ATT_ROOTID);
                           id_sep = getAttribute(event.asStartElement(), ATT_IDSEP);
                           default_exporter_start = getAttribute(event.asStartElement(), ATT_STARTTAG);
                           default_exporter_end = getAttribute(event.asStartElement(), ATT_ENDTAG);

                           // Get strings to replace in text
                           ElementLeaf.replacer.addPairs( getTextContent(xml_reader) );
                          }
                       else if ( isStartTag(event,TAG_ALIASES) )
                          {
                           // Collect  extension aliases
                           String ext_id = getAttribute(event.asStartElement(), ATT_ID);
                           String ext_al = getTextContent(xml_reader);
                           extaliases.put(ext_id, ext_al);
                           // Substitute input file extension if is an alias
                           if ( !all_langs && ext_al.contains(ext) ) ext = ext_id;
                          }
                       else if ( isEndTag(event,TAG_COMMON) ) status = all_langs ? Status.GET_ALLLANGS : Status.GET_LANG;
                       break;

                   case GET_ALLLANGS :
                       // Support nested languages
                       if ( isStartTag(event,TAG_LANG) )
                          {
                           String id = getAttribute(event.asStartElement(), ATT_ID);
                           Map<String,ElementReader> elreaders = getLang(xml_reader, id + id_sep);
                           langs.put( id, elreaders );
                          }
                       else if ( isEndTag(event,TAG_ROOT) ) status = Status.ALL_DONE;
                       break;

                   case GET_LANG :
                       // Find a 'TAG_LANG' that matches input extension
                       if ( isStartTag(event,TAG_LANG) )
                          {
                           String id = getAttribute(event.asStartElement(), ATT_ID);
                           if ( id.equalsIgnoreCase(ext) )
                              {
                               Map<String,ElementReader> elreaders = getLang(xml_reader, id + id_sep);
                               langs.put( id, elreaders );

                               status = Status.ALL_DONE;
                              }
                          }
                       break;
                  } // end 'switch status'
              } // end 'all XML events (until break)'
           } // end 'try'
        finally { xml_reader.close(); xml_reader = null; }

        // . . . Final things
        if ( status!=Status.ALL_DONE && all_langs ) throw new Exception("Malformed config file, check it!");
    } // end 'getReaders'


    /**
     *  Parse a <lang> content from the XML config file
     *  @param   xr         XML file parser
     *  @param   idprfx     Readers ID lang prefix
     *  @param   chargroups Predefined character groups
     *  @return             An ElementReader instance
     *  @throws  Exception  Typically if config file is bad
     *  @see                ElementReader
     */
    public Map<String,ElementReader> getLang(XMLEventReader xr, String idprfx) throws Exception
    {
        // . . . Local variables
        String reader_id = "";
        boolean nested_only = false;
        String nested = "";
        Detector start = null;
        Detector end = null;
        String exporter_start = "";
        String exporter_end = "";

        Status prev_status = status;// Remember previous status
        status = Status.GET_READERS;
        Map<String,ElementReader> elreaders = new LinkedHashMap<String,ElementReader>();

        // . . . Get
        while ( xr.hasNext() && status!=Status.FINAL_THINGS )
          {
           XMLEvent event = xr.nextEvent(); // nextTag();
           //System.out.println("New XML event>\t" + event.toString()); // D EBUG

           switch (status)
              {
               case GET_READERS :
                   // Read all 'TAG_READER'
                   if ( isStartTag(event,TAG_READER) )
                      {
                       // Get the reader id and other attributes
                       reader_id = getAttribute(event.asStartElement(), ATT_ID).trim();
                       if ( reader_id.isEmpty() ) throw new Exception("A <" + TAG_READER + "> has no attribute " + ATT_ID);
                       nested_only = getAttribute(event.asStartElement(), ATT_TYPE).trim().equalsIgnoreCase(VAL_SUBREGION);
                       nested = getAttribute(event.asStartElement(), ATT_NESTED).trim();
                       exporter_start = getAttribute(event.asStartElement(), ATT_STARTTAG);
                       exporter_end = getAttribute(event.asStartElement(), ATT_ENDTAG);

                       //System.out.println("\t>> Reader: " + reader_id); // D EBUG

                       // Initialize other required ingredients
                       start = null;
                       end = null;
                       status = Status.GET_READER;
                      }
                   else if ( isEndTag(event,TAG_LANG) ) status = Status.FINAL_THINGS;
                   break;

               case GET_READER :
                   // Two tags expected, the start and stop conditions,
                   // from which must retrieve two detectors
                   // Actually must deal with the special case of
                   // Keyword detector, for which the start detector
                   // owns the real end detector, the other is just fake
                   if ( start == null ) status = Status.GET_DETECTOR;
                   else {
                         if ( start instanceof KeywordDetector && end!=null )
                            {
                             ((KeywordDetector) start).assignEndDetector(end);
                             end = null;
                            }
                         // If not end detector provided, set a default
                         if (end == null) end = new FixedDetector(true);

                         // Create a new ElementReader from tag attributes and content
                         //System.out.println("New reader " + reader_id); // D EBUG
                         ElementReader er = new ElementReader(idprfx + reader_id, start, end, exporter_start, exporter_end, nested_only);
                         er.nested_bag = nested; // store nested list
                         elreaders.put( reader_id, er );
                         status = Status.GET_READERS;
                        }
                   break;

               case GET_DETECTOR :
                   // Get reader start and end detector
                   if ( isStartTag(event,TAG_START) )
                        {
                         start = getDetector(xr, event.asStartElement(), chargroups );
                         status = (end==null) ? Status.GET_DETECTOR : Status.GET_READER;
                        }
                   else if ( isStartTag(event,TAG_END) )
                        {
                         end = getDetector(xr, event.asStartElement(), chargroups );
                         status = (start==null) ? Status.GET_DETECTOR : Status.GET_READER;
                        }
                   else if ( isEndTag(event,TAG_READER) )
                        {
                         if (start==null) throw new Exception("Invalid config file: a <" + TAG_READER + "> has no <" + TAG_START + ">");
                         status = Status.GET_READER;
                        }
                   else if ( isEndTag(event,TAG_LANG) ) throw new Exception("Invalid config file: unable to read <" + TAG_READER + "id=\"" + reader_id + "\"");

                   break;
              } // end 'switch status'
          } // end 'all next XML events until break'

        // Check
        if ( status != Status.FINAL_THINGS ) throw new Exception("Malformed config file, check it! All <" + TAG_READER + "> must contain a <" + TAG_START + "> and a <" + TAG_END + ">");

        // Resolve nested readers within lang
        if ( elreaders.isEmpty() )
             {
              System.err.println("(!) No highlighting rules found for lang " + idprfx);
             }
        else {
              // For each reader...
              for( Map.Entry<String,ElementReader> entry: elreaders.entrySet() )
                {
                 ElementReader e = entry.getValue();

                 // Set default exporter tag/att
                 if (e.start_pattern.isEmpty()) e.start_pattern = default_exporter_start;
                 if (e.end_pattern.isEmpty()) e.end_pattern = default_exporter_end;

                 // Assign (possible) nested regions in this lang
                 if ( !e.nested_bag.isEmpty() )
                    {
                     String[] nested_names = e.nested_bag.split("[,;]+\\s*");
                     e.nested_bag = ""; // Now in nested bag I'll put unprocessed nested regions
                     for ( int i=0; i<nested_names.length; ++i )
                         {
                          String e_name = nested_names[i].trim();
                          if ( e_name.isEmpty() ) continue; // Ignore empty strings
                          //System.out.println("\tnested: " + name); // D EBUG
                          ElementReader ne = elreaders.get(e_name);
                          if ( ne != null ) e.AddNested( ne );
                          else e.nested_bag += e_name + " "; // could be a lang!
                         } // end 'for each nested name'
                     e.nested_bag = e.nested_bag.trim();
                    } // end 'nested names not empty'
                } // end 'for each reader'
             } // end 'there are highlighting rules'

        // . . . Finally
        status = prev_status;
        return elreaders;
    } // end 'getLang'


    /**
     *  Build the readers tree for a given file type
     *  @param ext         Extension of code snippet file
     *  @return            An ElementReader instance
     *  @throws Exception  Typically if config file is bad
     *  @see               ElementReader
     */
    public ElementReader BuildReader(String ext)
    {
        // . Resolve aliases
        int almatchnum = 0;
        for( Map.Entry<String,String> entry: extaliases.entrySet() )
           {
            if ( entry.getValue().contains(ext) )
               {
                if(++almatchnum>1) System.err.println("Warning: multiple aliases matching for extension " + ext);
                ext = entry.getKey();
               }
           }

        // . Create the root reader
        ElementReader root_reader = new ElementReader(ext+id_sep+exporter_root_id_suffix, new FixedDetector(true), new FixedDetector(false), exporter_root_start, exporter_root_end, false);

        // Create readers tree (assign nested element readers)
        Map<String,ElementReader> elreaders = langs.get(ext);
        if ( elreaders != null )
            {
             // For each lang reader...
             for( Map.Entry<String,ElementReader> entry: elreaders.entrySet() )
                {
                 ElementReader e = entry.getValue();
                 //System.out.println("> Got reader: " + e.id); // D EBUG

                 // Assign possible not processed nested regions (should be other langs)
                 if ( !e.nested_bag.isEmpty() )
                    {
                     String[] nested_names = e.nested_bag.split("[,;]+\\s*");
                     e.nested_bag = ""; // Now in nested bag I'll put unprocessed nested regions
                     for ( int i=0; i<nested_names.length; ++i )
                         {
                          String n_name = nested_names[i].trim();
                          if ( n_name.isEmpty() ) continue; // Ignore empty strings

                          // For each lang search
                          boolean lang_found = false;
                          for ( Map.Entry<String,Map<String,ElementReader>> lang_entry: langs.entrySet() )
                             {
                              Map<String,ElementReader> lang = lang_entry.getValue();
                              String lang_id = lang_entry.getKey();
                              //System.out.println("> Got lang: " + lang_id); // D EBUG
                              //for( Map.Entry<String,ElementReader> e_entry: lang.entrySet() ) System.out.println("> Got element: " + e_entry.getValue().id); // D EBUG
                              lang_found = n_name.equals(lang_id);
                              if ( lang_found )
                                 {
                                  // Assign lang readers as nested in this region
                                  for( Map.Entry<String,ElementReader> e_entry: lang.entrySet() ) e.AddNested( e_entry.getValue() );
                                  break;
                                 }
                             } // for all langs
                          if ( !lang_found ) e.nested_bag += n_name + " ";
                         } // end 'for each unprocessed nested name'
                     e.nested_bag = e.nested_bag.trim();
                     // All nested regions should now be processed
                     if ( !e.nested_bag.isEmpty() )
                        {
                         System.err.println("<" + TAG_READER + " id=\"" + e.id + "\"> claims to contain the unknown regions/lang: " + e.nested_bag + " (check misspelling or provide this region/lang in your cfg file)");
                        }
                    } // end 'nested names not empty'

                 // The root reader contains all defined readers
                 if ( !e.nested_only ) root_reader.AddNested( e );
                } // end 'for each reader'
            } // end 'got the lang related to extension'
       else {
             System.err.println("Cannot find a set of highlighting rules for extension \'" + ext + "\'");
             System.err.println("Edit the config file \'" + config_file.getName() + "\' adding an alias or a new lang");
            }

        return root_reader;
    } // end 'getReader'


    // . . . Some handy facilities
    /**
     *  Get the content of a XML tag
     *  @param   path   File path to xml config file
     *  @return         A String containing the contained characters
     *  @see            String
     */
    private String getTextContent(XMLEventReader xr) throws Exception // Collect node text content
    {
        String s = "";
        while ( xr.hasNext() && !xr.peek().isEndElement() )
           {
            XMLEvent e = xr.nextEvent();
            if (e.isCharacters()) s += e.asCharacters().getData();
           }
        return s; // s.trim();
    }

    /**
     *  States if a particular starting tag is encountered in XML file
     *  @param   e   XML event
     *  @param   n   A tag name
     *  @return      A boolean, true if the event correspond to that tag name
     *  @see         XMLEvent
     */
    private boolean isStartTag(XMLEvent e, String n)
    {
        return e.isStartElement() ? e.asStartElement().getName().toString().equals(n) : false;
    }

    /**
     *  States if a particular ending tag is encountered in XML file
     *  @param   e   XML event
     *  @param   n   A tag name
     *  @return      A boolean, true if the event correspond to that tag name
     *  @see         XMLEvent
     */
    private boolean isEndTag(XMLEvent e, String n)
    {
        return e.isEndElement() ? e.asEndElement().getName().toString().equals(n) : false;
    }

    /**
     *  Get a particular attribute of a XML start element
     *  @param   e   XML start element
     *  @param   a   Attribute name
     *  @return      A String, empty if attribute not found
     *  @see         StartElement
     */
    private String getAttribute(StartElement e, String a)
    {
        Attribute att = e.getAttributeByName(new QName(a));
        return att!=null ? att.getValue() : "";
    }

    /**
     *  Get a CharGroup object from a XML start element
     *  @param   e   XML start element
     *  @return      A CharGroup object, null if not found
     *  @see         CharGroup
     */
    private CharGroup getCharGroup(XMLEventReader xr, StartElement e, Map<String,CharGroup> chargroups) throws Exception
    { // Create a new CharGroup from tag attributes and content

        // Check if there is a tag
        XMLEvent next = xr.peek();
        if ( isStartTag(next, TAG_CHARGROUP) ) e = xr.nextTag().asStartElement();

        // Get element name
        String tag = e.getName().toString();

        // Get content
        String content = getTextContent(xr).trim();
        //System.out.println(">> Getting CharGroup: " + content); // D EBUG
        
        CharGroup cg = null;

        // State if dealing with dedicated tag or not
        if ( tag.equals(TAG_CHARGROUP) )
             {
              String id = getAttribute(e, ATT_ID);
              if ( content.isEmpty() )
                   { // Search existing in map
                    cg = chargroups.get(id);
                    if ( cg == null ) throw new Exception("The " + TAG_CHARGROUP + " with " + ATT_ID + " = " + id + " is undefined");
                   }
              else {
                    boolean negate = getAttribute(e, ATT_OPTS).equalsIgnoreCase(VAL_NOT);
                    cg = negate ? new CharGroupNot(content,id) : new CharGroup(content,id);
                   }
             }
        else { // Retrieve from content
              cg = new CharGroup(content);
             }
        //if (cg!=null) System.out.println("\t\t>> CharGroup: " + cg.getId()); // D EBUG
        return cg;
    }

    /**
     *  Get a Detector object from a XML start element
     *  @param   e   XML start element
     *  @return      A Detector object, null if not found
     *  @see         Detector
     */
    private Detector getDetector(XMLEventReader xr, StartElement e, Map<String,CharGroup> chargroups ) throws Exception
    {
        // Get element name
        String tag = e.getName().toString();

        // Deal with options
        boolean included = tag.equals(TAG_START); // default: true only for start detectors
        boolean nocase = false; // used only for 'KeywordDetector'
        String opts = getAttribute(e, ATT_OPTS).trim();
        if ( !opts.isEmpty() )
           {
            if ( opts.equalsIgnoreCase(VAL_INCLUDED) ) included = true;
            else if (opts.equalsIgnoreCase(VAL_EXCLUDED) ) included = false;
            else if (opts.equalsIgnoreCase(VAL_NOCASE) ) nocase = true;
           }

        // Deal with 'type'
        Detector d = null;
        String type = getAttribute(e, ATT_TYPE).trim();
        if ( type.equalsIgnoreCase(VAL_CHAR) ) d = new CharDetector(getCharGroup(xr,e,chargroups), included);
        else if ( type.equalsIgnoreCase(VAL_CHARNUM) ) d = new CharNumDetector(Integer.parseInt(getTextContent(xr).trim()));
        else if ( type.equalsIgnoreCase(VAL_STRING) ) d = new StringDetector(getTextContent(xr), nocase, included);
        else if ( type.equalsIgnoreCase(VAL_REGEX) ) d = new RegexDetector(getTextContent(xr).trim(), included);
        else if ( type.equalsIgnoreCase(VAL_KEYWORDS) ) d = new KeywordDetector(getTextContent(xr), nocase, included);
        else throw new Exception("Unknown <" + tag + "> type " + type);

        //if (d!=null) System.out.println("\t\t>> Detector: " + d.getClass().getName()); // D EBUG
        return d;
    }

 // . . . Attributes
    // Retrieved
    Map<String,Map<String,ElementReader>> langs; // Language readers map
    Map<String,CharGroup> chargroups; // Defined character classes
    Map<String,String> extaliases; // Extensions aliases
    String exporter_root_start = "";
    String exporter_root_end = "";
    String exporter_root_id_suffix = "";
    String id_sep = "";
    String default_exporter_start = "";
    String default_exporter_end = "";
    // Auxiliary
    enum Status { START, GET_COMMON, GET_LANG, GET_ALLLANGS, GET_READERS, GET_READER, GET_DETECTOR, FINAL_THINGS, ALL_DONE }
    private Status status = Status.START;
    private File config_file = null;

    // Tag names
    private static final String TAG_ROOT = "src2xml-config";
    private static final String TAG_EXPORTER = "exporter";
    private static final String TAG_COMMON = "common";
    private static final String TAG_CHARGROUP = "chargroup";
    private static final String TAG_LANG = "lang";
    private static final String TAG_READER = "region";
    private static final String TAG_START = "start";
    private static final String TAG_END = "end";
    private static final String TAG_ALIASES = "aliases";
    // Tag attributes
    private static final String ATT_ID = "id";
    private static final String ATT_ROOT_STARTTAG = "root-start-tag";
    private static final String ATT_ROOT_ENDTAG = "root-end-tag";
    private static final String ATT_ROOTID = "root-id-suffix";
    private static final String ATT_IDSEP = "id-separator";
    private static final String ATT_STARTTAG = "start-tag";
    private static final String ATT_ENDTAG = "end-tag";
    private static final String ATT_NESTED = "contains";
    private static final String ATT_OPTS = "opts";
    private static final String VAL_NOT = "not";
    private static final String VAL_INCLUDED = "included";
    private static final String VAL_EXCLUDED = "excluded";
    private static final String ATT_TYPE = "type";
    private static final String VAL_CHAR = "char";
    private static final String VAL_CHARNUM = "charnum";
    private static final String VAL_STRING = "string";
    private static final String VAL_REGEX = "regex";
    private static final String VAL_KEYWORDS = "keyword";
    private static final String VAL_SUBREGION = "subregion";
    private static final String VAL_NOCASE = "case-insensitive";
}

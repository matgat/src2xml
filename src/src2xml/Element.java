package src2xml;

// Containers
//import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  Element is an abstract representation of a region
 *  in the source code.
 *  Its specializations can contain other nested regions
 *  or consist just in a plain String.
 *
 *  @author Matteo Gattanini
 *  @see ElementReader
 */
public abstract class Element
{
    /**
     *  Class constructor
     *  @param  s_id   An IDentificative String for this region
     *  @see           ElementNode
     *  @see           ElementLeaf
     */
    public Element(String s_id) {id = s_id;}

 // . . . Main interface
    /**
     *  Print the region XML representation,
     *  surrounded by a tag
     *  @return            The tagged String
     *  @throws Exception  If something's wrong
     *  @see               java.lang.String
     */
    abstract public String Print() throws Exception;

    /**
     *  A debug facility that shows the Elements tree
        in the standard output stream
     *  @param   lvl   Just to keep track of the level in the tree
     */
    abstract public void Show(int lvl); // Debug

 // . . . Attributes
    public String id;
}


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  An element that contains other neasted elements
 *
 */
class ElementNode extends Element
{
    /**
     *  Class constructor
     *  @param   sid       An IDentificative String for this region
     *  @param   startptn  Starting pattern surrounding region - use var (id)
     *  @param   endptn    Ending pattern surrounding region - use var (id)
     *  @see               Element
     */
    public ElementNode(String s_id, String startptn, String endptn)
    {
        super(s_id);

        // . . . Build the exporter tags
        start_tag = startptn.replace("(id)", s_id);
        end_tag = endptn.replace("(id)", s_id);
        //if ( !tag.isEmpty() )
            // {
             // start_tag = att.isEmpty() ? "<" + tag + ">" : "<" + tag + " " + att + "=\"" + id + "\">";
             // end_tag = "</" + tag + ">";
            // }
    }

 // . . . Main interface
    /**
     *  Recursively call the nested elements method
     *  @throws Exception  If something's wrong
     *  @return            The tagged String
     */
    public String Print() throws Exception
    {
     StringBuilder s = new StringBuilder(start_tag);
     for ( Element child : childs ) s.append( child.Print() );
     return s.append(end_tag).toString();
    }

    /**
     *  Call recursively for nested Elements
     *  @param   lvl   The level in the tree
     */
    public void Show(int lvl){String t=""; for(int i=0; i<lvl; ++i) t+="\t"; System.out.println(t+"|- "+id); for(Element child : childs) child.Show(lvl+1);} // Debug

 // . . . Specializations
    /**
     *  Add a nested Element
     *  @param   e   Nested element to add
     */
    public void addChild(Element e) { childs.add(e); }

 // . . . Attributes
    //public List<Element> childs = new LinkedList(); // Sub-regions
    private Collection<Element> childs = new LinkedList<Element>(); // Sub-regions
    private String start_tag="", end_tag="";
}


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  An element that contains just a plain String
 *
 */
class ElementLeaf extends Element
{
    /**
     *  Class constructor
     *  @param   sid   An IDentificative String for this region
     *  @see           Element
     */
    public ElementLeaf(String s) 
    {
        super("");
        id = "";
        content = s;
    }

 // . . . Main interface
    /**
     *  Return the plain text content, possibly replacing
     *  XML spacial characters with the related entities
     *  @throws Exception  If something's wrong
     *  @return        The region plain content
     */
    public String Print() throws Exception
    {
        return replacer.doReplace(content);
    }

    /**
     *  Show that I am a leaf node
     *  @param   lvl   The level in the tree
     */
    public void Show(int lvl){String t=""; for(int i=0; i<lvl; ++i) t+="\t"; System.out.println(t+"|- <plain string>");} // Debug
 
 // . . . Attributes
    private String content; // The contained string
    static public StringUtils.MultipleReplacer replacer = new StringUtils.MultipleReplacer();
}
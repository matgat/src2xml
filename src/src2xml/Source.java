package src2xml;

// Containers
import java.util.Deque;
import java.util.ArrayDeque;
// IO
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;



//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  A class that implements a BufferedReader
 *  wrapping a FileReader.
 *  The Reader standard interface is too restricting,
 *  specially the mark() reset() part, very dangerous
 *  if calls are inadvertently nested.
 *  This class uses a stack to support nested marks.<p>
 *  Limitations:
 *    <ul>
 *     <li> Buffers in memory the whole file!
 *     <li> Uses the not thread safe StringBuilder
 *    </ul>
 *
 *  @author Matteo Gattanini
 *  @see Reader
 */
public class Source // MyBufferedReader extends Reader
{
    /**
     *  Class constructor
     *  @param   path       The input file path
     *  @throws  Throwable  If something's wrong during file buffering
     *  @see                StringBuilder
     */
    public Source(String path) throws Throwable
    {
        try {
             System.out.println("");
             System.out.println("Reading from " + path);
             reader = new FileReader(path);
            }
        catch(IOException e)
            {
             System.err.println("Unable to open file: " + e.getMessage());
            }

        // Now buffer whole file
        buf = new StringBuilder(CHUNK_SIZE);
        char[] chunk = new char[CHUNK_SIZE];
        int readchars = 0;
        while( (readchars = reader.read(chunk)) > 0 ) buf.append(chunk, 0, readchars);
        chunk = null;
    }
    /**
     *  Class destructor
     *  Ensure release of used resources
     */
    @Override protected void finalize() throws Throwable
    {
        close();
        buf = null;
        marks = null;
        super.finalize();
    }

 // . . . Casts
    @Override public String toString() {return buf.toString();}
    public CharSequence asCharSequence() {return buf;}

 // . . . Main interface
    /**
     *  Point the next char in source.
     *  @return    returns false if no more
     */
    public boolean nextChar() { return ++idx < buf.length(); }

    /**
     *  Get the current char.
     *  @return    returns the current pointed char
     */
    public char currChar()
    {
        try { return buf.charAt(idx); }
        catch (IndexOutOfBoundsException e) {return INVALID_CHAR;}
    }

    /**
     *  Get the current char index in source.
     *  @return    returns the current char index
     */
    public int currCharIdx() {return idx;}

    /**
     *  Peek the next char in source.
     *  @return    returns the peeked char, INVALID_CHAR if source ended
     */
    public char peekChar()
    {
        try { return buf.charAt(idx+1); }
        catch (IndexOutOfBoundsException e) {return INVALID_CHAR;}
    }

    /**
     *  Read a fixed length String from source
     *  @param   len    String length to read
     *  @return         The read String
     *  @see            String
     */
    public String read(int len)
    {
        String s = buf.substring(idx , Math.min(idx+len, buf.length()));
        idx += len;
        return s;
    }

    /**
     *  Check source end
     *  @return         Returns true if source ended
     */
    public boolean Ended() { return idx >= buf.length(); }

    /**
     *  Mark source position.
     *  Pushes the current character index in a stack.
     */
    public void setMark() { marks.push(idx); }

    /**
     *  Restore a previously marked source position.
     *  Pops the last mark from stack overwriting current character index.
     */
    public void restoreMark() { assert(marks.size()>0) : "call setMark first!"; idx = marks.pop(); }

    /**
     *  Discard the previous mark.
     *  Pops the last mark from stack.
     */
    public void releaseMark() { assert(marks.size()>0) : "call setMark first!"; marks.pop(); }

    /**
     *  Ensure used resources deallocation.
     */
    private void close()
    {
        try { if (reader != null) { reader.close(); reader = null; } }
        catch(Exception e) { System.err.println("Closing failed: " + e.getMessage()); }
    }

 // . . . Attributes
    // Constants
    /**
     *  Chunk size when buffering file [chars]
     */
    public static final int CHUNK_SIZE = 32768;
    public static final char INVALID_CHAR = (char) -1;
    // Auxiliary
    private int idx = 0; // Current character index (0 - buf.length()-1)
    private StringBuilder buf = null; // internal buffer
    private Reader reader = null;
    private Deque<Integer> marks = new ArrayDeque<Integer>(); // Marks container to support nesting
} // end 'class Source'
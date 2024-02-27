/**
 *  A Class to deal with file writing
 *
 *  @author Matteo Gattanini
 *  @see java.io.Writer
 */

package src2xml;

// IO
import java.io.File;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;


//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// Read a file
class OutputWriter
{
// . . . Main services
    /**
     *  Class constructor
     *  @param f    file to write
     */
    public OutputWriter(File f) { out_file = f; }
    public OutputWriter(String p) { out_file = new File(p); }

    /**
     *  Class destructor
     *  @throws Throwable  If something wrong
     */
    @Override protected void finalize() throws Throwable
    {
        close();
        super.finalize();
    }

    // . . . Main interface

    /**
     *  Overwrite/create a file with a given String
     *  @param s             The String to write
     *  @throws IOException  If an output exception occurred
     */
    public void Write(String s) throws IOException
    {
        try {
             System.out.println("Writing to " + out_file.getPath());
             writer = new BufferedWriter( new FileWriter(out_file) );
             writer.write( s );
             writer.flush();
            }
        finally { close(); }
    }

    /**
     *  Overwrite/create a file with a given Stream
     *  @param s             The String to write
     *  @throws IOException  If an exception occurred while reading from stream
     */
    public void Write(InputStream s) throws IOException
    {
        assert (s != null) : "Attempt to write a null input stream!";
        Reader reader = new InputStreamReader(s);
        try {
             System.out.println("Writing to " + out_file.getPath());
             writer = new BufferedWriter( new FileWriter(out_file) );
             // Transfer
             char[] chunk = new char[CHUNK_SIZE];
             int readchars = 0;
             while( (readchars = reader.read(chunk)) > 0 ) writer.write(chunk, 0, readchars);
             reader = null;
             writer.flush();
            }
        finally { close(); }
    }

    /**
     *  Ensure to close the used resource
     */
    private void close()
    {
        try { if (writer != null) {writer.close(); writer = null;} }
        catch(Exception e) { System.err.println("Closing file failed: " + e.getMessage()); }
    }

// . . . Attributes
    public static final int CHUNK_SIZE = 32768;
    private File out_file = null;
    private Writer writer = null;
} // end 'OutputWriter'

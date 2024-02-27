package src2xml;

// IO
import java.io.File;
// Containers
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
/**
 *  The application Main class.
 *  @author  Matteo Gattanini
 */
public class Main {

    /**
     *  The application main function.
     *    It can be divided in the following parts:
     *    <ul>
     *     <li> 1) Retrieving command line arguments
     *     <li> 2) Prepare input files list
     *     <li> 3) Create the CSS file if necessary
     *     <li> 4) Prepare region readers from  XML configuration file
     *     <li> 5) Process all input files
     *    </ul>
     * @param args   the command line arguments
     */
    public static void main(String[] args)
    {
        //System.out.println( System.getProperty("java.version") );

        // . (1) Retrieve args
        //System.out.println("Passed args:"); for(int i=0; i<args.length; ++i) System.out.println("\t" + args[i]); // D EBUG
        ArrayList<String> inpaths = new ArrayList<String>();
        ArrayList<File> outpaths = new ArrayList<File>();
        File css_file = null;
        boolean recurse = false;
        String outdefext = ".html"; // Output default extension
        try{
            for(int i=0; i<args.length; ++i)
               {
                //System.out.println(args[i]); // D EBUG
                if ( args[i].charAt(0) == OPT_CHAR )
                     {
                      // An option: retrieve code and argument
                      String[] parts = args[i].split("=");
                      //for(int j=0; j<parts.length; ++j) System.out.println("\t"+parts[j]); // D EBUG
                      if ( parts.length==0 || parts.length>2 ) throw new Exception("Invalid option " + args[i]);
                      String opt_code = parts[0].substring(1);
                      String opt_arg = (parts.length > 1) ? parts[1] : "";
                      if ( opt_code.equalsIgnoreCase(OPT_OUT) )
                         {
                          if ( opt_arg.isEmpty() ) throw new Exception("Must specify file path in " + args[i]);
                          outpaths.add(new File(opt_arg));
                         }
                      else if ( opt_code.equalsIgnoreCase(OPT_CSS) )
                         {
                          if ( opt_arg.isEmpty() ) throw new Exception("Must specify file path in " + args[i]);
                          css_file = new File(opt_arg);
                         }
                      else if ( opt_code.equalsIgnoreCase(OPT_CFG) )
                         {
                          if ( opt_arg.isEmpty() ) throw new Exception("Must specify file path in " + args[i]);
                          config_file = new File(opt_arg);
                         }
                      else if ( opt_code.equalsIgnoreCase(OPT_EXT) )
                         {
                          if ( opt_arg.isEmpty() ) throw new Exception("Must specify file extension in " + args[i]);
                          outdefext = opt_arg.startsWith(".") ? opt_arg : "."+opt_arg;
                         }
                      else if ( opt_code.equalsIgnoreCase(OPT_REC) ) recurse = true;
                      else throw new Exception("Unknown option " + args[i]);
                     }
                else inpaths.add(args[i]);
               } // end 'for each arg'
            if ( inpaths.isEmpty() ) throw new Exception("No input files defined");
           }
        catch (Exception e)
           {
            System.err.println(e.getMessage());
            System.err.println("");
            System.err.println("Usage: java -jar src2xml.jar  [paths] [options]");
            System.err.println("Examples:");
            System.err.println("java -jar src2xml.jar  source.cpp");
            System.err.println("java -jar src2xml.jar  source.cpp -out=source.html -cfg=\"c:\\my conf\\myconfig.xml\"");
            System.err.println("java -jar src2xml.jar  \"Sources\\*.js\" -out=\"Dest\\\"");
            System.err.println("java -jar src2xml.jar  \"c:\\my files\\source.cpp\" -out=\"c:\\my out\\source.html\" -css=\"c:\\my out\\style.css\" -cfg=myconfig.xml");
            System.exit(ERR_ARGS);
           }

        try{
            // . (2) Prepare input files list
            // Inputs
            ArrayList<File> infiles = new ArrayList<File>();
            for(String inpath : inpaths)
               {
                File f = new File(inpath);
                String GlobPattern = f.isDirectory() ? "*" : f.getName();
                FileUtils.RegexFilenameFilter fl = new FileUtils.RegexFilenameFilter(StringUtils.Glob2RegEx(GlobPattern));
                //System.err.println(inpath + ": " + GlobPattern + ": "+StringUtils.Glob2RegEx(GlobPattern));
                infiles.addAll(FileUtils.FileList( new File(inpath), fl, recurse ));
               }
            //for (File infile : infiles) System.err.println(infile.getName()); System.err.println("");

            // . (3) Creating css file
            OutputWriter wCss = null;
            if ( css_file != null )
               {
                if ( css_file.exists() )
                      System.out.println("Using existing CSS file: " + css_file.getName());
                else {
                      wCss = new OutputWriter(css_file);
                      wCss.Write( "/* Do not edit! File generated by src2xml */" + eol); 
                     }
               }

            // . (4) Build the element readers from config file
            HashMap<String,ElementReader> readers = new HashMap<String,ElementReader>();
            try {
                 ConfigurationGetter conf = new ConfigurationGetter(Main.config_file);
                 for (File infile : infiles)
                    {
                     String inext = FileUtils.ExtractExt(infile.getName());
                     if(!readers.containsKey(inext))
                        {
                         ElementReader reader = conf.BuildReader(inext);
                         readers.put(inext,reader);
                         // Append to CSS file if requested
                         if(wCss!= null) wCss.Write(eol+reader.generateCSS());
                        }
                    }
                }
            catch (Exception e)
                {
                 System.err.println(e.getMessage());
                 System.err.println("Invalid config file! Fix it, otherwise delete it so that I can create a new one");
                 System.exit(ERR_CONFIG);
                }

            // . (5) Process all input files
            Iterator<File> outpathsI = outpaths.iterator();
            File outpath = new File(".");
            File outfile;
            for (File infile : infiles)
                {
                 // . Prepare the reader
                 String inext = FileUtils.ExtractExt(infile.getName());
                 ElementReader reader = readers.get(inext);

                 // . Read the inputs source file and fill regions tree
                 Element root = reader.Read( new Source(infile.getAbsolutePath()) ); //root.Show(0); // D EBUG

                 // . Write output file
                 // Select the output file path
                 if(outpaths.isEmpty()) outfile = new File(infile.getAbsolutePath()+outdefext);
                 else if(outpathsI.hasNext()) outfile=outpath=outpathsI.next();
                 else outfile = outpath;
                 if(outfile.isDirectory()) outfile = new File(FileUtils.JoinPaths(outfile.getAbsolutePath(),infile.getName()+outdefext));
                 String outext = FileUtils.ExtractExt(outfile.getName());

                 // Create output file according to out extension
                 StringBuilder sb = new StringBuilder();
                 if ( outext.equalsIgnoreCase("html") )
                      {
                       sb.append("<!DOCTYPE html>" + eol + "<html>" + eol);
                       sb.append("<!-- Do not edit! File generated by src2xml -->" + eol);
                       sb.append("<head>" + eol + "<title>" + infile.getName() + "</title>" + eol);
                       sb.append("<meta name=\"keywords\" content=\"" + inext + ",code,source,snippet\"/>" + eol);
                       sb.append("<meta name=\"description\" content=\"A " + inext + " source code snippet\"/>" + eol);
                       if(css_file!=null) sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + css_file.getPath() + "\"/>" + eol);
                       else sb.append("<style type=\"text/css\">" + eol + reader.generateCSS() + "</style>" + eol );
                       sb.append("</head>" + eol + "<body>" + eol + root.Print() + "</body>" + eol + "</html>");
                      } // end 'writing html'
                 else {
                       //sb.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>" + eol);
                       sb.append( root.Print() );
                      }

                 // Write operation
                 OutputWriter w = new OutputWriter(outfile);
                 w.Write(sb.toString());
                } // end 'for each input file'

            // . . . Finally
            System.out.println("...Done");
           }
        catch(Throwable e)
           {
            System.err.println(e.getMessage());
            // e.printStackTrace(); // D EBUG
            System.exit(ERR_OP);
           }
    } // end 'main'

 // . Application attributes
 // Settings
    private static final char OPT_CHAR = '-';
    private static final String OPT_CSS = "css";
    private static final String OPT_CFG = "cfg";
    private static final String OPT_OUT = "out";
    private static final String OPT_REC = "r";
    private static final String OPT_EXT = "ext";
    private static File config_file = new File("src2xml-config.xml");
    public static final String eol = System.getProperty("line.separator");
 // Embedded
    public static final String resPath = "res/src2xml-config.xml";
 // Compiling
     //public static final boolean _DEBUG = false;
 // Return values
    public static final int ERR_ARGS = 2;
    public static final int ERR_CONFIG = 3;
    public static final int ERR_OP = 4;
}

package src2xml;

// Containers
import java.util.ArrayList;
// Regex
import java.util.regex.Pattern;
//import java.util.regex.Matcher;
// Sys
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
//import java.io.FileNotFoundException;




/////////////////////////////////////////////////////////////////////////////
/**
 *  This class collects some File facilities
 *  @author Matteo Gattanini
 *
 */
public class FileUtils
{
 // . . . Attributes
 //static private MultipleReplacer single_escseqs_replacer = new MultipleReplacer("(\\\\,\\) (\\t,\t) (\\n,\n) (\\r,\r) (\\f,\f) (\\b,\b) (\\\',\') (\\\",\")");

 // . . . Methods

    //-----------------------------------------------------------
    /**
     *  Extract file extension
     *  @param  fname        The input file path
     *  @return              The extension
     *  @see                 java.lang.String
     */
  public static String ExtractExt(String fname)
     {
      return fname.substring(fname.lastIndexOf('.')+1).toLowerCase();
     }

    //-----------------------------------------------------------
    /**
     *  Relativize path
     *  @param  base         The input base path
     *  @param  path         The path to relativize
     *  @return              The relativized path
     *  @see                 java.lang.String
     */
    public static String RelativizePath(String base,String path)
    {
     if(path.startsWith(base)) path=path.substring(base.length());
     return path;
     //return new File(base).toURI().relativize(new File(path).toURI()).getPath();
    }


    //-----------------------------------------------------------
    /**
     *  Join two paths
     *  @param  base         The input base path
     *  @param  path         The tail path
     *  @return              The joined path
     *  @see                 java.lang.String
     */
    public static String JoinPaths(String base,String path)
    {
     // . Ensure trailing separator/strip file name from base
     int slash = base.lastIndexOf(File.separatorChar);
     if( new File(base).isDirectory() )
          {
           if(slash!=(base.length()-1)) base+=File.separator;
          }
     else {
           if(slash>0) base = base.substring(0,slash+1);
           else return path;
          }

     // . Remove undesired starting chars from path (../)
     if(path.startsWith("."+File.separator)) path = path.substring(2);
     else if(path.startsWith(".."+File.separator)) path = path.substring(3);
     while(path.startsWith(File.separator)) path = path.substring(1);

     return base+RelativizePath(base,path);
    } // end 'JoinPaths'


  /**
   * Return a file with the given filename creating the necessary
   * directories if not present
   *
   * @param filename The file to create
   * @return The created File instance
   */
  public static File EnsureFile(String fname) throws IOException
     {
      File file = new File(fname);
      File parent = file.getParentFile();
      if (parent!=null) parent.mkdirs();
      file.createNewFile(); // if true file did not exist and was created, else file already exists
      return file;
     } // end 'EnsureFile'




  /**
  * Recursively walk a directory tree and return a List
  * of all Files found matching a filter (no directories)
  *
  * @param aStartingDir a valid directory, which can be read
  * @return The created File List
  */
  public static ArrayList<File> FileList( File f, FilenameFilter filt, boolean recurse )
     {
      //if(filt==null) filt = new FilenameFilter() {public boolean accept(File dir, String name) {return true;}}
      ArrayList<File> result = new ArrayList<File>();
      if(f.isDirectory())
         {
          File[] currlist = f.listFiles(filt);
          for (int i=0; i<currlist.length; ++i)
             {
              if (currlist[i].isFile()) result.add(currlist[i]);
              else if(recurse) result.addAll(FileList(currlist[i],filt,recurse));
             }
         }
      else if(filt.accept(new File(f.getParent()), f.getName())) result.add(f);
      return result;
     } // end 'FileList'



    /////////////////////////////////////////////////////////////////////////
    /**
     *  A class that implements a regexp file filter
     *
     */
    static public class RegexFilenameFilter implements FilenameFilter
    {
        /**
         *  Class constructor
         *  @param regex the regular expression of the filter
         *  @see                 java.util.regex.Pattern
         */
        public RegexFilenameFilter(String regex) throws java.util.regex.PatternSyntaxException
        {
            i_pattern = Pattern.compile(regex);
        }

        /**
        Tests if a specified file should be included in a file list
        @param dir the directory in which the file was found
        @param name the name of the file
        @return true if the name passes the filter
        */
        public boolean accept(File dir, String name)
        {
          //return name.matches(i_regex);
          return i_pattern.matcher(new File(name).getName()).matches();
        }

        // . . . Attributes
        private Pattern i_pattern;
    } // end class 'RegexFilenameFilter'


//    /////////////////////////////////////////////////////////////////////////
//    /**
//     *  A class that implements a glob file filter
//     *
//     */
//    //import java.nio.file.FileSystem;
//    static public class GlobFilenameFilter implements FilenameFilter
//    {
//        /**
//         *  Class constructor
//         *  @param regex the regular expression of the filter
//         *  @see                 java.util.regex.Pattern
//         */
//        public GlobFilenameFilter(String ptrn)
//        {
//            i_matcher = FileSystem.getDefault().getPathMatcher("glob:" + ptrn);
//        }

//        /**
//        Tests if a specified file should be included in a file list
//        @param dir the directory in which the file was found
//        @param name the name of the file
//        @return true if the name passes the filter
//        */
//        public boolean accept(File dir, String name)
//        {
//          return i_matcher.matches(new File(name).getName());
//        }

//        // . . . Attributes
//        private PathMatcher i_matcher;
//    } // end class 'GlobFilenameFilter'


} // end class 'FileUtils'

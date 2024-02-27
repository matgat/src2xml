/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package packagename;

// Containers
import java.util.Map;
import java.util.HashMap;

/**
 * @author User
 */
public class code
{

    public code()
    {
     boolean b = false;
     char c = '\u4A3F';
     String s = "ciao\n \u1234 uella\r\n";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        System.out.println("Passed args:");
        for(int i=0; i<args.length; ++i) System.out.println("\t"+args[i]);

        Map<String,String> tokens = new HashMap<String,String>();
        tokens.put("cat", "Garfield");

        // Build regular expression
        StringBuilder regex = new StringBuilder("(");
        for (String k : tokens.keySet()) regex.append(k+"|"); regex.setCharAt(regex.length()-1, ')');
        System.out.println(regex);
    }
    
    /**
     *  Class destructor
     *  @throws Throwable  If something wrong
     */
    @Override protected void finalize() throws Throwable
    {
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while ( i++ > 10  )
           {
            sb.append("ciao\n");
           }

        System.out.println(sb.toString());
    }
}

/**
 *  An utility that transforms a source code file
 *  to a xml file for highlighting purposes.
 *  This is a SBVEH (Stupid But Very Expandable Highlighter),
 *  an inefficient but very customizable structure.
 *  <p>
 *  Features/shortcomings:
 *  <ul>
 *     <li> The highlight rules are chosen by input file
 *          extension.
 *     <li> A remarkable freedom on highlighter customization:
 *          add new languages, new regions, etc...
 *          Peek the XML config file (automatically created if missing)
 *          you should then figure out how to obtain advanced
 *          highlighting features
 *     <li> Supports multi syntax highlight in the same file
 *     <li> Edit yourself a proper CSS file to style the
 *          source code as you prefer, starting from the
 *          template produced with option {@code -css}
 *     <li> Specify "html" extension in output file name to
 *          obtain a ready to use html file, otherwise use xml
 *          extension
 *     <li> When using regular expressions with lookbehind
 *          (ex in html lang definition) the parsing takes
 *          a long looong time...
 *  </ul>
 *
 *  Usage:<p> {@code java -jar src2xml.jar  [paths] [options]}
 * <p>
 * Command line arguments can be:
 *  <ul>
 *     <li> {@code path}             Add a file/folder in the input files list;
 *                                   in case of file names you can use wildcards
 *                                   (glob patterns)
 *     <li> {@code -r}               Recursively process input subfolders
 *     <li> {@code -out=path}        Add a file/folder in the output files list
 *     <li> {@code -cfg=config-file} Explicitly indicate a configuration file
 *     <li> {@code -css=css-file}    Generate (if not existing) a dummy stylesheet
 *                                   file (use as a template); in case of html
 *                                   output also links to this stylesheet
 *     <li> {@code -ext=file-ext}    Select output mode by file extension
 *  </ul>
 *
 *  Examples:
 *  <ul>
 *     <li>Obtain {@code code.html} from a cpp source:<p>
 *          {@code java -jar src2xml.jar "C:\my code\code.cpp" -out=code.html}
 *     <li>More files:<p>
 *          {@code java -jar src2xml.jar code1.cpp -out=code1.html code2.cpp -out=code2.html}
 *     <li>Transform all files in a directory, recursively<p>
 *          {@code java -jar src2xml.jar "Snippets\" -out="Dest\" -r -ext=xml}
 *  </ul>
 *
 *  @author   Matteo Gattanini
 *  @version  0.3
 */

package src2xml;
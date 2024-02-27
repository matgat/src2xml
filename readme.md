## [src2xml](https://github.com/matgat/src2xml.git)

A tool that transforms source code into xml for code highlighting.

> My first and last application written in Java, developed using
> Netbeans around 2004 to colorize the source code snippets in my
> [thesis](https://github.com/matgat/thesis.git).
> Archived here for historical reasons.


### Features/shortcomings
* The highlight rules are chosen by input file extension.
* A remarkable freedom on highlighter customization: add new languages, new regions, etc... Peek the XML config file (automatically created if missing) you should then figure out how to obtain advanced highlighting features
* Supports multi syntax highlight in the same file
* Edit yourself a proper CSS file to style the source code as you prefer, starting from the template produced with option -css
* Specify "html" extension in output file name to obtain a ready to use html file, otherwise use xml extension
* When using regular expressions with lookbehind (ex in html lang definition) the parsing takes a long looong time...


### Usage
    $ java -jar src2xml.jar [paths] [options]

#### Command line arguments

| Argument           | Description |
|--------------------|-------------|
| `path`             | Add a file/folder in the input files list; in case of file names you can use wildcards (glob patterns) |
| `-r`               | Recursively process input subfolders |
| `-out=path`        | Add a file/folder in the output files list |
| `-cfg=config-file` | Explicitly indicate a configuration file |
| `-css=css-file`    | Generate (if not existing) a dummy stylesheet file (use as a template); in case of html output also links to this stylesheet |
| `-ext=file-ext`    | Select output mode by file extension |


### Examples

Obtain code.html from a cpp source:

    $ java -jar src2xml.jar "C:\my code\code.cpp" -out=code.html


More files:

    $ java -jar src2xml.jar code1.cpp -out=code1.html code2.cpp -out=code2.html


Transform all files in a directory, recursively:

    $ java -jar src2xml.jar "Snippets\" -out="Dest\" -r -ext=xml

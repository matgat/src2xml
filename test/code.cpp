//=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
#pragma hdrstop
//#include <cstdlib>
#include <iostream> // Stream facilities
// Classes "commented string"

const String s1a = "a double quoted string";
const SysUtils::String s2a = 'a single quoted string';
const std::string s3a = "a string \'with\' \"quotes\"";
int i = 123;
static double charlie = 3.14;
	char cint = 'd', c2 = '\t';
char c3 = '\n';
char &c4 = c3;
#define MACRO(x,y) x_y  /* a macro int */ ehh?
// Some escape sequences
{ \xAB, \xABXX, \nXX, \rXX, \v, \uACCD, \uABCDXXXX }
xxx\nxxx xxx\uABCDxxx

label:
aaa bbb
/*
 multiline comment
 int a = 0; // commented code

 "quoted string in comment"
 an unicode character \u0074

  \n	New line
  \t	Tab
  \b	Backspace
  \r	Carriage return
  \f	Formfeed
  \\	Backslash
  \'	Single quotation mark
  \"	Double quotation mark
  \176	Octal
  \x7E	Hexadecimal
  \u007E	Unicode character

 */
//---------------------------------------------------------------------------
// A buffer template
template <class T> class cls_Buffer
{
public:
    __fastcall cls_Buffer(unsigned int siz) : i_size(siz) { i_buf = new T[i_size]; }
    __fastcall ~cls_Buffer() {delete[] i_buf;}
    T operator[](const int i)const {return i_buf[i];}
    T* get()const {return i_buf;}
    __property int size = {read = i_size};
private:
    unsigned int i_size; T* i_buf;
};

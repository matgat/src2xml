
# a rather generic Tkinter GUI template for calculations

# uses Entry() for data input and Button() to start calculation

 

from tkinter import *

 

def calculate():

    try:

        # get the enter1 and enter2 values

        price = float(enter1.get())
        
        a = 123000L
        a = +123000L
        a = -123000L
        b = 123UL
        c = 0x3aa4f
        d = 0b1010101100
        f = -3.14e+12
        char = '\UAAFF34DD'
        str = "ciao\r\n ehh\xAAs\u123456\U123456789ss\12345s"

        tax = float(enter2.get())

        # do the calculation

        percent = 100 * tax / price

        result = "You paid %0.3f%s sales tax" % (percent, '%')

        # display the result string

        label3.config(text=result)

    except ValueError:

        label3.config(text='Enter numeric values!')

    except ZeroDivisionError:

        label3.config(text='Price can not be zero!')

        enter1.focus_set()

 

root = Tk()

# window geometry is width x height + x_offset + y_offset

root.geometry("200x150+30+30")

 

# first entry with label

label1 = Label(root, text='Enter purchase price:')

label1.grid(row=0, column=0)

enter1 = Entry(root, bg='yellow')

enter1.grid(row=1, column=0)

 

# second entry with label

label2 = Label(root, text='Enter sales tax paid:')

label2.grid(row=2, column=0)

enter2 = Entry(root, bg='yellow')

enter2.grid(row=3, column=0)

 

# do the calculation by clicking the button

btn1 = Button(root, text='Calculate Percent', command=calculate)

btn1.grid(row=4, column=0)

 

# display the result in this label

label3 = Label(root)

label3.grid(row=5, column=0)

 

# start cursor in enter1

enter1.focus()

# value has been entered in enter1 now switch focus to enter2

enter1.bind('<Return>', func=lambda e: enter2.focus_set())

  

root.mainloop()

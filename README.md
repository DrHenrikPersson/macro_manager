# macro_manager
Macro Manager is a plugin for ImageJ to manage your own macros and allow simple execution and editing.  
Original plugin writte in April 2020  
Repository created 2020-10-06
  
## Installation
1. Download the file macro_manager.java
2. Move it to .../imageJ/plugins/Macro Manager
3. Run imageJ
4. Click *Plugins=>Compile and Run/*  
ImageJ should compile the code and add the compiled files to your .../imageJ/plugins/Macro Manager folder. The plugin should automatically show up in ImageJ's plugin menu next time you use run ImageJ.

## General instructions
The first time you run the plugin, you will be asked if you want to create a *macroManagerSettings.txt* file. Allow the program to do this. This file will store the paths to all your macros which you have to either add one by one through the GUI menues or all at once manually in the settings textfile, by typing in the paths.  

## Detailed instructions
### General usage
Click **button with macro name** to run macro.  
The macro code will be read when the button is clicked and will reflect changes made to macro. I.e. no need to restart plugin for changes to take effect.  
Click **edit button** to open imageJ macro editor and make any changes you see fit. Save. Any saved changes will be used next time the macro is run. (No restart required.)
	
### Customize macro list  
- *File=>Add macro*
	- Use dialogue to navigate to macro
- *File=>Delete macro(s)*
	- Use dialogue to delete macro(s)
	- Check which macros to DELETE 
-*File=>Edit macro list*
	- Shows txt file with macro paths. Can be used to add or delete manually. 
	- Use refresh GUI to see changes.
- *File=>Refresh GUI*
	- Updates GUI by re-reading settings file
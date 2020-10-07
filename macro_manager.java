/*
Macro Manager is an ImageJ plugin to manage your own macros and allow simple execution and editing through a small GUI.

Author: Henrik Persson
2019-08-12, plugin created
Uploaded to GitHub 2020-10-06
Last update 2020-10-06
*/

//import statements
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.frame.*;

import javax.swing.*;
import javax.swing.filechooser.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.Scanner;
import java.util.Vector;

import java.awt.*;
import java.awt.event.*;
import java.awt.Checkbox;

public class macro_manager implements PlugIn{

	/*
	* ---------------------------------------------------
	* Variables
	* ---------------------------------------------------
	*/
	//macro paths are stored in macroManagerSettings.txt
	final String settingsPath=IJ.getDir("plugins") + "Macro Manager\\macroManagerSettings.txt";
	
	//GUI parameters
	JFrame mainFrame;
	JPanel btnPanel;
	macro[] macroArray;//array to store macros
	//Size settings for GUI
	int gap = 5;//gap for buttons etc
	int frameWidth = 275;
	int baseHeight = 50;
	int framHeightPerButton = 35;
	
	//about this version
	String version = "1.1";//version name
	String updatedDate = "2020-10-06";//date of last update
	String author="Henrik Persson";//name of author
	
	/*
	* ---------------------------------------------------
	* run()
	* 
	* Main function to generate GUI
	* Executed when plugin is started 
	* ---------------------------------------------------
	*/
	public void run(String arg){
		//create macro objects from paths in settings file, store in  macroArray
		boolean settingsFileExists = false;
		//check if settings file exists, otherwise prompt to create
		try{
			settingsFileExists = checkSettingsFile();
		}
		catch(IOException e1){
			IJ.showMessage("Exception thrown by checkSettingsFile(), called from run()." + 
			"\nFile could not be found or could not be created");
		}	
		if(settingsFileExists)
			refreshGui();//refreshGui works on first creation as well
	}
	
	/*
	* ---------------------------------------------------
	* checkSettingsFile()
	* 
	* Check that settingsfile exists, otherwise create it
	* 
	* returns boolean: True if file exists or was created
	* throws IOException
	* ---------------------------------------------------
	*/
	private boolean checkSettingsFile() throws IOException{
		File settingsFile = new File(settingsPath);
		boolean exists = settingsFile.exists();
		if (!exists){
			//settingsFIle does not exist, prompt creation
			int create = 0;
			JFrame createFrame = new JFrame("File missing");
			create=JOptionPane.showConfirmDialog(createFrame, "Settings file does not exist in " + IJ.getDir("plugins") + "\n Create?");
			
			if(create==0){
				PrintWriter pw = new PrintWriter(settingsFile);
				pw.println("");
				pw.close();
				IJ.showMessage("File created");
				exists=true;
			}
			else{
				IJ.showMessage("Required settings file not found. Please create file \n" +
				settingsPath + "\nor allow Macro Manager to create it for you. \n"+
				"Plugin will exit now.");
				exists=false;				
			}
		}//if settingsFile !exists
	return exists;
	}//checkSettingsFile()
	
	/*
	* ---------------------------------------------------
	* readLines()
	* 
	* read lines from file to retrieve macro paths
	* 
	* returns String[] containing macroPaths
	* throws Exception
	* ---------------------------------------------------
	*/
	private String[] readLines() throws Exception{
		File file = new File(settingsPath);
		//count number of lines in file
		Scanner scCounter = new Scanner(file);
		int lines=0;
		while (scCounter.hasNextLine()) {
			scCounter.nextLine();
			lines++;
		}
		
		//read lines from file, store in String array
		Scanner scReader =  new Scanner(file);
		String[] macroPaths = new String[lines];
		int index=0;
		while (scReader.hasNextLine()) {
			macroPaths[index++]=scReader.nextLine();
		}
		return macroPaths;
	}//readlines
	
	/*
	* ---------------------------------------------------
	* populateMacroArray()
	*
	* Create macro objects based on paths in settings file
	* Store in macroArray	
	* ---------------------------------------------------
	*/
	private void populateMacroArray(){
		try{
			String[] macroPaths = readLines();
			macroArray = new macro[countValidMacros(macroPaths)];
			int index=0;
			for(int i=0;i<macroPaths.length;i++){
				if(isValidMacro(macroPaths[i]))
					macroArray[index++]=new macro(macroPaths[i]);
			}
		}
		catch(Exception e){
			IJ.showMessage("Exception thrown by readLines(), called from populateMacroArray()");
		}    
	}//populateMacroArray

	/*
	* ---------------------------------------------------
	* countValidMacros()
	* 
	* Count number of valid macros in String array
	* 
	* input String[]: containing all macro paths
	* returns int
	* ---------------------------------------------------
	*/	
	private int countValidMacros(String[] macroPaths){
		int count = 0;
		for(int i=0; i<macroPaths.length;i++){
			if(isValidMacro(macroPaths[i]))
				count++;
		}
		return count;
	}

	/*
	* ---------------------------------------------------
	* isValidMacro()
	* 
	* Check if the given path leads to a valid macro file
	* Macro counts as valid if it ends in '.txt' or '.ijm'
	* 
	* input String: containing path to macro
	* return Boolean: true if valid macro path
	* ---------------------------------------------------
	*/	
	private Boolean isValidMacro(String macroPath){
		File tmpMacro = new File(macroPath);
		String[] validFileExtensions= {".txt", ".ijm"};
		if(tmpMacro.exists()){
			for(int i=0; i<validFileExtensions.length;i++){
				if(getExtension(tmpMacro).compareTo(validFileExtensions[i]) == 0){
					return true;
				}//if
			}//for
		}//exists
		//either not valid file or not possessing valid extension
		return false;
	}//isValidMacro
	
	/*
	* ---------------------------------------------------
	* getExtension()
	* 
	* Return file extension as string
	* 
	* input File: the file to be examined
	* returns String: containing extension
	* ---------------------------------------------------
	*/	
	private String getExtension(File macroFile){
	    String fName = macroFile.getName();
		int lastIndexOf = fName.lastIndexOf(".");//get last period
		if (lastIndexOf == -1)
			return ""; // empty extension
    return fName.substring(lastIndexOf);//split at last period
	}
	
	/*
	* ---------------------------------------------------
	* refreshGui()
	* 
	* Refresh GUI by removing previous version (if any) and rebuilding it
	* ---------------------------------------------------
	*/
	private void refreshGui(){
		//reload macro path list
		try{
			populateMacroArray();
			}
		catch(Exception e3){
			IJ.showMessage("Exception thrown by populateMacroArray(), called from refreshGui().");
		} 
		
		//get location of mainFrame if it exists. Otherwise, use imageJ window to derive GUI coordinates
		int[] windowLocation = new int[2];
		try{
			Point mainFramePoint = mainFrame.getLocation();
			windowLocation[0] = (int)mainFramePoint.getX();
			windowLocation[1]= (int)mainFramePoint.getY();
			mainFrame.setVisible(false);
		} catch(Exception e){
		//Could not get macro_manager GUI location, use ImageJ's location instead
			windowLocation=getWindowLocation();	
		}

		//check which buildGui() function to use, based on whether there are macros in the list or not
		int noMacros=0;
		try{
			String[] macroPaths = readLines();
			noMacros = countValidMacros(macroPaths);
		}catch(Exception e){
			IJ.showMessage("Readlines exception, called from run()");
		}
		//int[] windowLocation=getWindowLocation();
		if(noMacros>0)
			buildGui(windowLocation[0],windowLocation[1]);
		else
			buildGuiNoMacros(windowLocation[0],windowLocation[1]);//no macros in file, create empty GUI
	}
	
	/*
	* ---------------------------------------------------
	* getWindowLocation()
	* 
	* Calculate coordinates for window location 
	* based on imageJ's current location and specified frame widht of macro_manager
	* 
	* retruns int[]: contains (x,y) coordinates for top left corner of macro_manager GUI
	* ---------------------------------------------------
	*/
	private int[] getWindowLocation(){
		
		int[] coordinates= new int[2];
		
		Point imageJMainWindowPoint = IJ.getInstance().getLocation();
		int iJWidth = IJ.getInstance().getWidth();
		int iJHeight = IJ.getInstance().getHeight();
		coordinates[0] = (int)imageJMainWindowPoint.getX()+iJWidth-frameWidth;
		coordinates[1] = (int)imageJMainWindowPoint.getY()+iJHeight;
		
		return coordinates;
	}
	
	/*
	* ---------------------------------------------------
	* buildGui()
	* 
	* Construct GUI by coordinating function calls
	* Used by refreshGUI to ensure refreshed GUI is located where previous one was (if moved)
	* 
	* input int: x coordinate, top left corner
	* 		int: y coordinate, top left corner
	* ---------------------------------------------------
	*/
	private void buildGui(int xLocation, int yLocation){
		mainFrame = new JFrame("Macro manager");
		mainFrame.setLayout(new GridLayout(0, 1, gap, gap));
		JPanel btnPanel = buildBtnPanel();
		JMenuBar mb = buildJMenuBar();

		mainFrame.add(btnPanel);
		mainFrame.setJMenuBar(mb);
		
		mainFrame.setSize(frameWidth,baseHeight+framHeightPerButton*(macroArray.length));
		mainFrame.setLocation(xLocation, yLocation);
		
		mainFrame.setVisible(true);
	}//buildGui
	
	/*
	* ---------------------------------------------------
	* buildGuiNoMacros()
	* 
	* No valid macros in file, create empty GUI prompting user to add some
	* 
	* input int: x coordinate, top left corner
	* 		int: y coordinate, top left corner
	* ---------------------------------------------------
	*/
	private void buildGuiNoMacros(int xLocation, int yLocation){
		mainFrame = new JFrame("Macro manager");
		mainFrame.setLayout(new GridLayout(0,1, gap, gap));

		JPanel emptyPanel = new JPanel();
		JLabel emptyLabel0=new JLabel(" ");
		JLabel emptyLabel1=new JLabel("There are no valid macros in the settings file.");
		JLabel emptyLabel2=new JLabel("Add a few from the file menu.");
		emptyPanel.add(emptyLabel0);
		emptyPanel.add(emptyLabel1);
		emptyPanel.add(emptyLabel2);
		
		JMenuBar mb = buildJMenuBar();
		mainFrame.add(emptyPanel);
		mainFrame.setJMenuBar(mb);
		
		mainFrame.setSize(frameWidth,baseHeight*4);
		mainFrame.setLocation(xLocation, yLocation);
		
		mainFrame.setVisible(true);
		
	}//buildGuiNoMacros
	
	/*
	* ---------------------------------------------------
	* buildBtnPanel()
	* 
	* Create JPanel with macro buttons for buildGui()
	* 
	* returns JPanel: JPanel with macro buttons
	* ---------------------------------------------------
	*/
	private JPanel buildBtnPanel(){
		
		btnPanel=new JPanel();
		btnPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;//stretch buttons to fill space
		c.insets=new Insets(1,2,1,2);//padding around all buttons
		
		//button arrays
		JButton[] btns = new JButton[macroArray.length];
		JButton[] editBtns = new JButton[macroArray.length];
		
		//create pair of launch and edit buttons for each macro in list
		for(int row = 0; row < macroArray.length; row++){
			final String tmpMacroPath = macroArray[row].getPath();
			File tmpFile = new File(tmpMacroPath);
			
			//create launch button
			btns[row]=new JButton(macroArray[row].getName());
			btns[row].addActionListener(new ActionListener(){  
				public void actionPerformed(ActionEvent e){  
					//execute macro from path
					IJ.runMacroFile(tmpMacroPath);
				}  
			});
			
			//launch button alignment
			btns[row].setHorizontalAlignment(SwingConstants.LEFT);			
					
			//update constraints, add launch button
			c.gridx=0;
			c.gridy=row;
			c.weightx=1;
			c.weighty=1;
			btnPanel.add(btns[row],c);
			
			//create edit button
			editBtns[row]=new JButton("Edit");
			editBtns[row].addActionListener(new ActionListener(){  
				public void actionPerformed(ActionEvent e){  
					//edit macro 
					IJ.run("Edit...", "open=["+tmpMacroPath+"]");//code to edit the macroPath at work. Brackets, [], are needed around path
				}  
			});
			
			//update constraints, add edit button
			c.gridx=1;			
			c.gridy=row;
			c.weightx=.3;
			c.weighty=1;
			btnPanel.add(editBtns[row], c);
		}// for macro lines
		return btnPanel;
	}//buildBtnPanel()
	
	/*
	* ---------------------------------------------------
	* buildJMenuBar()
	* 
	* Build and return menubar for buildGui()
	* 
	* return JMenuBar: JMenuBar with menus
	* ---------------------------------------------------
	*/
	private JMenuBar buildJMenuBar(){
		JMenuBar mb = new JMenuBar();
		
		/*
		* Create file menu
		*/
		JMenu filemenu = new JMenu("File");
		JMenuItem itemAddMacro;
		JMenuItem itemDelMacro;
		JMenuItem itemEditSettings;
		JMenuItem itemRefresh;
		JMenuItem itemNewCode;
		
		//create menu items
		
		//'add macro' menu item
		itemAddMacro = new JMenuItem("Add macro");
		itemAddMacro.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent eItemAddMacro){
				addMacroGui();
			}
		});

		//'delete macro(s)' menu item
		itemDelMacro = new JMenuItem("Delete macro(s)");
		itemDelMacro.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent eItemDelMacro){
				deleteMacro();
			}
		});
		
		//'edit macro' list menu item
		itemEditSettings = new JMenuItem("Edit macro list");
		itemEditSettings.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent eEditSettings){
				IJ.run("Edit...", "open=["+settingsPath+"]");
			}
		});
		
		//'refresh GUI' menu item
		itemRefresh = new JMenuItem("Refresh GUI");
		itemRefresh.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent eRefresh){
				refreshGui();
			}
		});
		
		//add menu items to menu
		filemenu.add(itemAddMacro);
		filemenu.add(itemDelMacro);
		filemenu.add(itemEditSettings);
		filemenu.add(itemRefresh);
		
		/*
		* Create help menu
		*/
		JMenu helpMenu = new JMenu("Help");
		JMenuItem itemInstructions;
		JMenuItem itemAbout;
		
		//'instructions'  menu item
		itemInstructions = new JMenuItem("Instructions");
		itemInstructions.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent eInstructions){
				showInstructions();
			}
		});
		
		//'About' menu item
		itemAbout = new JMenuItem("About");
		itemAbout.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent eAbout){
				showAbout();
			}
		});
		
		//add menu items
		helpMenu.add(itemInstructions);
		helpMenu.add(itemAbout);
		
		//add menus to menubar
		mb.add(filemenu);
		mb.add(helpMenu);
		return mb;
	}//buildJMenuBar

	/*
	* ---------------------------------------------------
	* addMacroGui()
	* 
	* Add single macro using dialog by adding path to settingsfile and rebuilding GUI
	*
	* ---------------------------------------------------
	*/
	private void addMacroGui(){
		//create a file chooser to be displayed in a frame
		final JFrame frameOpen = new JFrame();
		final JFileChooser fc = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Macro files only", "txt","ijm");
		fc.setFileFilter(filter);
		fc.setCurrentDirectory(new File(IJ.getDir("macros")));
		
		//select file, write file path to settings file
		int returnVal = fc.showOpenDialog(frameOpen);
			if(returnVal == JFileChooser.APPROVE_OPTION){
				File file = fc.getSelectedFile();
				try{
					FileWriter fw = new FileWriter (settingsPath, true);//create filewriter. append = true
					PrintWriter pw = new PrintWriter(fw);//create PrintWriter, acts as interface to fileWriter
					
					pw.print(file.getAbsolutePath());//print on new line, no carriage return at end
					pw.println("");//new line
					pw.close();
					
					//refresh GUI
					refreshGui();
				}
				catch(IOException e1){
					IJ.showMessage("Exception thrown, trying to write to file" + settingsPath);
				}	
			}//if file selection approved
			else {//file selection canceled
				//IJ.showMessage("No macro selected.");
			}
	}//addMacro()

	/*
	* ---------------------------------------------------
	* deleteMacro()
	* 
	* Delete macro(s) using check list selection
	* List of amcros with check boxes is created
	* un-checked macros are kept and the settingsFile is rebuilt using these
	* Refresh GUI afterwards
	* ---------------------------------------------------
	*/
	private void deleteMacro(){
		
		//Generate dialogbox, populate with macro labels
		GenericDialog deleteDialog = new GenericDialog("Select macros to delete");
		for(int i=0; i<macroArray.length;i++){
			deleteDialog.addCheckbox(macroArray[i].getName(), false);	
		}
		deleteDialog.showDialog();	
		
		//if user clicked OK, get list of checkbox, delete checked macros
		if(deleteDialog.wasOKed()){
			Vector macrosToDelete = deleteDialog.getCheckboxes();
			
			//count number of unchecked boxes (macros to save)
			int noMacrosToKeep=0;
			for(int k=0;k<macrosToDelete.size();k++){
				Checkbox chk = (Checkbox)macrosToDelete.get(k);//retrieve checkbox from Vector
				if(!chk.getState())
				//checkbox was not checked => keep macro
					noMacrosToKeep++;
			}
			
			if(noMacrosToKeep==0){
				//delete all macros
				writeMacroToFile(" ", false); //overwrite file with empty string
			}else{
				//save at least one macro
				String[] macrosToSave = new String[noMacrosToKeep];
				int index=0;
				boolean first = true;//first macro to save: clear settings file
				for(int j=0;j<macrosToDelete.size();j++){
					Checkbox chk = (Checkbox)macrosToDelete.get(j);//retrieve checkbox from Vector
					if(!chk.getState()){
						//Store paths of macros to save in array//send array to writer to store in settings file
						if(first){
							//first macro to save, clear file and add macro
							writeMacroToFile(findMacro(chk.getLabel()).getPath(), false);
							first=!first;
						}
						else{
							//not first macro, append to file
							writeMacroToFile(findMacro(chk.getLabel()).getPath(), true);
						}//else
					}//if first
				}//for
			}//else
			//update GUI
			refreshGui();
		}//if ok was clicked
	}
	
	/* 
	* ---------------------------------------------------
	* findMacro()
	* 
	* Find macro in macroArray based on its name. 
	* Use with delete function to find its path
	* Will return first occurence
	* 
	* input String: name of macro
	* returns macro
	* ---------------------------------------------------
	*/
	private macro findMacro(String name){
		for(int i=0; i<macroArray.length;i++){
			if (macroArray[i].getName() == name)
				return macroArray[i];
		}
		return null;
		
	}//findmacro
	
	/*
	---------------------------------------------------
	* writeMacroToFile()
	* 
	* Write macro path to file, single macro
	* 
	* input String: macro path to write to file
	* 		Boolean:	true: append to file; 
	* 					false: create new file
	---------------------------------------------------
	*/	
	private void writeMacroToFile(String path, Boolean append){
		try{
			FileWriter fw = new FileWriter (settingsPath, append);//create filewriter
			PrintWriter pw = new PrintWriter(fw);//create PrintWriter, acts as interface to fileWriter
			pw.print(path);//print on new line, no carriage return at end
			pw.println("");//new line
			pw.close();
		}
		catch(IOException e4){
			IJ.showMessage("Exception thrown, trying to write macroArray to file. In function writeMacroToFile(String path, Boolean append))");
		}
	}
	
	/*
	* ---------------------------------------------------
	* writeMacroArrayFile()
	* 
	* Write entire macroarray to the settingsfile by looping array and calling writeMacroToFile()
	* 
	---------------------------------------------------
	*/	
	private void writeMacroArrayToFile(){
		//write first macro to fresh file
		writeMacroToFile(macroArray[0].getPath(), false);
		//write subsequent macros to file while appending
		for(int i=1; i<macroArray.length;i++){
			writeMacroToFile(macroArray[i].getPath(), true);
		}
	}
	
	/*
	* ---------------------------------------------------
	* showInstructions()
	* 
	* Show instructions in help menu
	* 
	---------------------------------------------------
	*/		
	private void showInstructions(){
	String instructions = 
	"                           Use macros\n"+
	"*******************************************************************\n" + 
	"Click button with macro name to run macro.\n"+
	"The macro code will be read when the button is clicked and \n "+
	"will reflect changes made to macro.\n"+
	"I.e. no need to restart plugin for changes to take effect. \n"+
	" \n"+
	"Click edit button to open imageJ macro editor and make any \n" +
	"changes you see fit. Save. \n"+
	"Any saved changes will be used next time the macro is run."+
	" \n"+
	" \n"+
	" \n"+
	"                     Customize macro list\n" + 
	"*******************************************************************\n" + 
	"File=>Add macro\n" +
	"Use dialogue to navigate to macro\n" +
	" \n" + 
	"File=>Delete macro(s) \n" +
	"Use dialogue to delete macro(s) \n" +
	"Check which macros to DELETE \n"+
	" \n"+
	"File=>Edit macro list \n" +
	"Shows txt file with macro paths. Can be used to add or\n" + 
	"delete manually. Use \'refresh GUI\'  to see changes.\n"+
	" \n"+
	"File=>Refresh GUI \n" +
	"Updates GUI by re-reading settings file \n";

	IJ.showMessage("Instructions",instructions);
	
	}//show instructions
	
	/*
	* ---------------------------------------------------
	* showAbout()
	* Show about message in help menu
	* 
	---------------------------------------------------
	*/		
	private void showAbout(){
		String aboutMessage = "Macro manager version " + version + "\n"+
				"Written by " + author+"\n"+
				"\nLast updated " + updatedDate;
		IJ.showMessage("About",aboutMessage);
	}

}//macro_manager class

/*
 * ---------------------------------------------------
 * Class macro
 * 
 * Inner class to store information about a macro
 * -----------------------------------------------------
 */
class macro{
	
	String path, name;

	//constructor with just path
	macro(String path){
		this.path=path;
		File tmpFile = new File(path);
		this.name=tmpFile.getName();
	}
	
	//constructor with name and path
	macro(String name, String path){
		this.name=name;
		this.path=path;
	}

	//get macro path
	public String getPath(){
		return this.path;
	}
	
	//get macro name
	public String getName(){
		return this.name;
	}
		
	
}//macro class
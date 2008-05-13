To compile the source files in this folder, you need to add the following EMF libraries
to your classpath:
org.eclipse.emf.common_<version>.jar
org.eclipse.emf.ecore_<version>.jar
org.eclipse.emf.ecore.xmi_<version>.jar

Then, in Eclipse you add the ecore_src folder to the source path of the uima_examples project
as follows:
- Open the Properties dialog for the uima_examples project. You can either "right click" on 
  the exmple project and select "Properties" from the menu, or select (highlight) the 
  uima_examples project then click "Project->Properties" from the main menu.
- Click on "Java Build Path" to open the build path panel.
- Click on the "Source" tab to see the source folders on the build path.
- Click "Add Folder..." and add "ecore_src" to the source folders build path.


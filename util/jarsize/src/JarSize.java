/*******************************************************************************
JarSize - Ant task to update the MIDlet-Jar-Size property inside a JAR file
Copyright (C) 2003  Manuel Linsmayer

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*******************************************************************************/


import java.io.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


public class JarSize extends Task
{


	// JAR file to update
    private File jarFile;
    private File jadFile;


    // Sets the file to update
    public void setJarFile(File file)
    {
		this.jarFile = file;
    }
    public void setJadFile(File file)
    {
		this.jadFile = file;
    }


    // Task implementation
    public void execute() throws BuildException
    {

		// Check whether file exists
    	if (!this.jarFile.isFile())
    	{
    		throw (new BuildException(jarFile.toString() + " does not exist or is not a file"));
    	}
    	if (!this.jadFile.isFile())
    	{
    		throw (new BuildException(jadFile.toString() + " does not exist or is not a file"));
    	}
	long jarSize = this.jarFile.length();

	try {
	    FileInputStream fis = new FileInputStream(jadFile);
	    DataInputStream input = new DataInputStream (fis);
	    StringBuffer jad = new StringBuffer();
	    String str = input.readLine();
	    while (null != str) {
		if (0 != str.length()) {
		    jad.append(str).append("\n");
		}
		str = input.readLine();
	    }
	    fis.close();
	    jad.append("MIDlet-Jar-Size: " + jarSize + "\n");
	    
	    FileOutputStream fos = new FileOutputStream(jadFile);
	    fos.write(jad.toString().getBytes());
	    fos.close();
	    System.out.println("MIDlet-Jar-Size property successfully updated");
	} catch (Exception e) {
	}
    }

}

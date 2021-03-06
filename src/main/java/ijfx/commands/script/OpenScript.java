/*
    This file is part of ImageJ FX.

    ImageJ FX is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ImageJ FX is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
    
     Copyright 2015,2016 Cyril MONGIS, Michael Knop
	
 */
package ijfx.commands.script;

import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Plugin;
import org.scijava.plugins.commands.io.OpenFile;

/**
 *
 * @author Cyril MONGIS
 */
@Plugin(type = ScriptCommand.class, menuPath = "File > Open...", menu = {
		@Menu(label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
			mnemonic = MenuConstants.FILE_MNEMONIC),
		@Menu(label = "Open...", weight = 1, mnemonic = 'o', accelerator = "^O") },
	attrs = { @Attr(name = "no-legacy") })
public class OpenScript extends OpenFile implements ScriptCommand{
    
    
    
}

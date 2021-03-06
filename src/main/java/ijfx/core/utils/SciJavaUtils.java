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
package ijfx.core.utils;

import ijfx.ui.display.tool.LineTool;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

/**
 *
 * @author Cyril MONGIS
 */
public class SciJavaUtils {
    public static String getLabel(SciJavaPlugin plugin) {
        
        return plugin.getClass().getAnnotation(Plugin.class).label();
        
    }
    
    public static String getName(SciJavaPlugin plugin) {
        return plugin.getClass().getAnnotation(Plugin.class).name();
        
    }
    
    public static String getIconPath(SciJavaPlugin plugin) {
        return plugin.getClass().getAnnotation(Plugin.class).iconPath();
        
    }

    public static String getDescription(SciJavaPlugin plugin) {
        return plugin.getClass().getAnnotation(Plugin.class).description();
    }
    
    public static double getPriority(SciJavaPlugin plugin) {
        return plugin.getClass().getAnnotation(Plugin.class).priority();
    }
}

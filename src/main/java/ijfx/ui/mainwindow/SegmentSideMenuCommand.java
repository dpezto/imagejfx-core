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
package ijfx.ui.mainwindow;

import ijfx.core.activity.ActivityService;
import ijfx.core.mainwindow.MainWindow;
import ijfx.core.uicontext.UiContextService;
import ijfx.core.uiplugin.AbstractUiCommand;
import ijfx.core.uiplugin.UiCommand;
import ijfx.ui.UiContexts;
import ijfx.ui.activity.DisplayContainer;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Cyril MONGIS
 */
@Plugin(type = UiCommand.class,label = "Segment",iconPath="fa:map_marker",priority = 0.6
,description="Work on the current images using ImageJFX segmentation tools")
public class SegmentSideMenuCommand extends AbstractUiCommand<MainWindow>{

    
    @Parameter
    UiContextService contextService;
    
    @Parameter
    ActivityService activityService;
    
    public SegmentSideMenuCommand() {
        super(MainWindow.class);
    }

   

    @Override
    public void run(MainWindow t) {
        
        contextService.enter(UiContexts.SEGMENT);
        contextService.update();
        
        activityService.open(DisplayContainer.class);
        
       
        
    }
    
}

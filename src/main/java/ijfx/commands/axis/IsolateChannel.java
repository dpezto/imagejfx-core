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
package ijfx.commands.axis;

import ijfx.core.batch.CommandRunner;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplayService;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 *
 * @author Cyril MONGIS, 2016
 */
@Plugin(type = Command.class
        , menuPath = "Image > Color > Isolate channel"
        , initializer = "findCurrentChannel"
        , description = "Create a new image (or stack) composed uniquely of the current channel. All other dimensions are preserved."
)
public class IsolateChannel extends ContextCommand {

  
    @Parameter(type = ItemIO.INPUT)
    Dataset input;

    @Parameter(type = ItemIO.OUTPUT)
    Dataset output;

    @Parameter
    ImageDisplayService imageDisplayService;

    @Parameter(label = "Channel to isolate")
    int channel = -1;

    @Override
    public void run() {

        if (input.axis(Axes.CHANNEL).isPresent() == false) {
            cancel("the input image is not multichannel.");
            return;
        }

        output = new CommandRunner(getContext())
                .set("input", input)
                .set("position", channel)
                .set("axisType", Axes.CHANNEL)
                .runSync(Isolate.class)
                .getOutput("output");

    }

    public void findCurrentChannel() {

        if (channel == -1 && imageDisplayService.getActiveDataset() == input) {
            channel = imageDisplayService.getActiveDatasetView().getIntPosition(Axes.CHANNEL);
        }
        channel = channel == -1 ? 0 : channel;
    }

}

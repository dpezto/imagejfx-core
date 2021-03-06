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
package ijfx.core.uiplugin;

import java.util.List;
import java.util.stream.Collectors;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 *
 * @author Cyril MONGIS
 */
@Plugin(type = Service.class,priority = Priority.LOW_PRIORITY)
public class DefaultUiCommandService extends AbstractService implements UiCommandService {

    @Parameter
    PluginService pluginService;

    private List<UiCommand> getUiActionList() {

        return pluginService
                .createInstancesOfType(UiCommand.class);

    }

    @Override
    public <T> List<UiCommand<T>> getAssociatedAction(T o) {

        return getUiActionList()
                .stream()
                .filter(action -> action.canHandle(o.getClass()))
                .map(action -> (UiCommand<T>)action)
                .collect(Collectors.toList());

    }
    
    @Override
    public <T> List<UiCommand<T>> getAssociatedAction(Class<? extends T> type) {

        List<UiCommand> list =  getUiActionList();

        return list
                .stream()
                .filter(action -> action.canHandle(type))
                .map(action -> (UiCommand<T>)action)
                .collect(Collectors.toList());

    }

}

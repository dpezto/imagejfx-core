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
package ijfx.explorer;

import com.google.common.collect.Lists;
import ijfx.core.IjfxService;
import ijfx.core.datamodel.Iconazable;
import ijfx.core.imagedb.ImageRecord;
import ijfx.explorer.datamodel.Explorable;
import ijfx.explorer.views.ExplorerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javafx.beans.property.IntegerProperty;
import mongis.utils.task.ProgressHandler;

/**
 *
 * @author Cyril MONGIS, 2016
 */
public interface ExplorerViewService extends IjfxService,ExplorableViewModel {


    void setItems(List<? extends Explorable> items);

    void setOptionalFilter(Predicate<Explorable> addionnalFilter);

    default void selectItems(Explorable... items) {
        setSelected(Lists.newArrayList(items));
    }
    
    default void selectAll() {
        setSelected(getDisplayedItems());
    }

  
    void toggleSelection(Explorable explorable);

    /**
     * Open the iconazable displaying a loading screen
     *
     * @param iconazable to open
     */
    void open(Iconazable iconazable);

    void openSelection();

    IntegerProperty selectedCountProperty();

    public ArrayList<String> getMetaDataKey(List<? extends Explorable> items);

    public Stream<Explorable> indexDirectory(ProgressHandler origProgress, File directory);

    public Stream<Explorable> getSeries(ImageRecord explorable);
}

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
package ijfx.ui.filters.metadata;

import ijfx.core.metadata.MetaDataOwner;
import ijfx.ui.filter.DefaultNumberFilter;
import ijfx.ui.filter.NumberFilter;
import ijfx.ui.filter.NumberOwnerPredicate;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 *
 * @author Pierre BONNEAU
 */
public class NumberFilterWrapper<T extends MetaDataOwner> implements MetaDataOwnerFilter<T> {

    private final NumberFilter filter;
    private final Property<Predicate<T>> metaDataOwnerProperty;
    private String keyName;
    
    private Collection<? extends T> ownerList;
    
    public NumberFilterWrapper() {
        this(new DefaultNumberFilter());
    }

   
    
    

    public void setName(String keyName) {
        this.keyName = keyName;
    }
    
    
    
    
    public NumberFilterWrapper(NumberFilter filter) {
        this.filter = filter;
        this.metaDataOwnerProperty = new SimpleObjectProperty<>(null);

        if (filter.predicateProperty().getValue() != null) {
            metaDataOwnerProperty.setValue(new NumberOwnerPredicate(keyName, filter.predicateProperty().getValue()));
        }

        filter.predicateProperty().addListener(new ChangeListener<Predicate<Double>>() {
            @Override
            public void changed(ObservableValue<? extends Predicate<Double>> ov, Predicate<Double> t, Predicate<Double> newValue) {

                if (newValue != null) {
                    metaDataOwnerProperty.setValue(new NumberOwnerPredicate(keyName, newValue));
                } else {
                    metaDataOwnerProperty.setValue(null);
                }
            }
        });
    }

    @Override
    public Node getContent() {
        return filter.getContent();
    }

    @Override
    public Property<Predicate<T>> predicateProperty() {
        return this.metaDataOwnerProperty;
    }

    public NumberFilter getFilter() {
        return this.filter;
    }
    
    public String getName() {
        return keyName;
    }

    @Override
    public void setAllPossibleValues(Collection<? extends T> values) {
        

    }
}

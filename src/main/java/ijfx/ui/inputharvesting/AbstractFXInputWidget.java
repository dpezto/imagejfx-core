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
package ijfx.ui.inputharvesting;

import java.util.stream.Stream;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.scene.Node;
import org.scijava.widget.AbstractInputWidget;
import org.scijava.widget.WidgetModel;

/**
 *
 * @author Cyril MONGIS
 */
public abstract class AbstractFXInputWidget<T> extends AbstractInputWidget<T,Node>{
    
    
    ModelBinder<T> modelBinder = new ModelBinder<>(this);
    
    
    protected void bindProperty(Property<T> property) {
        modelBinder.bind(property);
        refreshWidget();
    }
    
    protected void bindProperty(ObjectProperty<T> property) {
        modelBinder.bind(property);
       
    }
    
  
    protected boolean isOneOf(Class<?> cl,Class<?>... classList) {
        return Stream.of(classList).filter(c->c.equals(cl)).count() > 0;
    }
    
    protected boolean isOneOf(WidgetModel model, Class<?>... classList) {
        
        
        
        try {
        return isOneOf(model.getItem().getType(),classList);
        }
        catch(NullPointerException npe) {
            for(Class<?> type : classList) {
                if(model.isType(type)) return true;
            }
        }
        return false;
    }
    
    public T getValue() {
        return modelBinder.getValue();
    }
    
    @Override
    public void refreshWidget() {
        T value = (T) get().getValue();
        modelBinder.refreshWidget(value);
    }
    
    @Override
    public Class<Node> getComponentType() {
        return Node.class;
    }
    
    
    
}

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
package ijfx.explorer.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

/**
 *
 * @author Cyril MONGIS
 */


public class DefaultTag implements Tag {

    final String name;

    @JsonCreator
    public DefaultTag(@JsonProperty("name") String name) {
        this.name = name;
    }

    @JsonGetter(value="name")
    @Override
   
    public String getName() {
        return name;
    }

    @Override
    public int compareTo(Tag o) {
        return name.compareTo(o.getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object object) {
        if (object instanceof Tag) {
            return ((Tag) object).getName().equals(getName());
        }

        if (object instanceof String) {
            ((String) object).equals(getName());
        }

        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}

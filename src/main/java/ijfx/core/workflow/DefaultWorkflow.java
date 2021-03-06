/*
 * /*
 *     This file is part of ImageJ FX.
 *
 *     ImageJ FX is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ImageJ FX is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
 *
 * 	Copyright 2015,2016 Cyril MONGIS, Michael Knop
 *
 */
package ijfx.core.workflow;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Cyril MONGIS, 2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultWorkflow implements Workflow {

   
    protected String name;

  
    protected String description;

    protected boolean mustBeStopped;

    
    protected List<WorkflowStep> steps = new ArrayList<>();
    
    public DefaultWorkflow() {
    }

    public DefaultWorkflow(List<WorkflowStep> steps) {
        this();
        setStepList(steps);
    }

    @Override
    @JsonGetter("name")
    public String getName() {
        return name;
    }
    
   

   
  

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonGetter("stepList")
    @Override
    public List<WorkflowStep> getStepList() {
        return steps;
    }


    public void setStepList(List<WorkflowStep> stepList) {
        steps.clear();
        steps.addAll(stepList);
    }

    @JsonSetter("stepList")
    public void serializeStepList(List<DefaultWorkflowStep> stepList) {

        //steps = stepList;
        steps.clear();
        steps.addAll(stepList);
        //stepList.forEach(step -> steps.add(step));

    }

    @Override
    public boolean mustBeStopped() {
        return mustBeStopped;
    }
    
    
    public void setMustBeStopped(boolean mustBeStopped) {
        this.mustBeStopped = mustBeStopped;
    }

}

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
package ijfx.ui.workflow;

import com.squareup.okhttp.internal.Internal;
import ijfx.core.workflow.WorkflowStep;
import ijfx.core.workflow.WorkflowStepWidgetModel;
import ijfx.ui.inputharvesting.InputPanelFX;
import ijfx.ui.main.ImageJFX;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.PopOver;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import mongis.utils.FXUtilities;
import mongis.utils.listcell.ListCellController;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;
import org.scijava.widget.WidgetService;

/**
 *
 * @author Cyril MONGIS, 2015
 */
public class WorkflowStepController extends BorderPane implements ListCellController<WorkflowStep> {

    //ModuleConfigPane configPane;
    InputPanelFX inputPanel;

    PopOver popover = new PopOver();

    @FXML
    Label titleLabel;

    @FXML
    Button configButton;

    @Parameter
    Context context;

    @Parameter
    WidgetService widgetService;

    public WorkflowStepController() {

        try {
            FXUtilities.injectFXML(this);
        } catch (IOException ex) {
            ImageJFX.getLogger().log(Level.SEVERE, null, ex);
        }

    }

    Consumer<WorkflowStep> deleteHandler;

    public Consumer<WorkflowStep> getDeleteHandler() {
        return deleteHandler;
    }

    public void setDeleteHandler(Consumer<WorkflowStep> deleteHandler) {
        this.deleteHandler = deleteHandler;
    }

    WorkflowStep step;

    @Override
    public void setItem(WorkflowStep step) {

        /*
        if (configPane == null) {
            configPane = new ModuleConfigPane();
            context.inject(configPane);
            
            popover.setContentNode(configPane);
            //popover.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);
            popover.setArrowLocation(PopOver.ArrowLocation.RIGHT_TOP);
        }*/
        
        if(this.step == step || step == null) return;
        inputPanel = new InputPanelFX();

        
        
        step
                .getParameters()
                .keySet()
                .stream()
                .map(str -> new WorkflowStepWidgetModel(context,step,str))
                
                .map((WidgetModel m)->{
                   InputWidget<? ,Node> widget = (InputWidget<?,Node>) widgetService.find(m);
                   if(widget != null) {
                       widget.set(m);
                       widget.refreshWidget();
                       return widget;
                   }
                   else {
                       return null;
                   }
                })
                .filter(widget->widget != null)
                .map(widget -> (InputWidget<?, Node>) widget)
                .forEach(inputPanel::addWidget);
        
        inputPanel.refresh();
        popover.setContentNode(inputPanel.getComponent());
        inputPanel.setName(
                step.getId() == null
                ? step.getModule().getInfo().getTitle()
                : step.getModule().getInfo().getTitle()
        );
        this.step = step;
        
        //configPane.configure(step);
        titleLabel.setText(step.getModule().getInfo().getTitle());
    }

    @Override
    public WorkflowStep getItem() {

        return step;
    }

    @FXML
    public void toggleConfigPane() {
        popover.setArrowLocation(PopOver.ArrowLocation.RIGHT_TOP);
        popover.show(configButton);
    }

    @FXML
    public void remove() {
        deleteHandler.accept(step);
    }

}

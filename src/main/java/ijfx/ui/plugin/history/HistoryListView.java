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
package ijfx.ui.plugin.history;

import ijfx.core.history.HistoryExecutorService;
import ijfx.core.history.HistoryService;
import ijfx.core.workflow.DefaultWorkflowStep;
import ijfx.core.workflow.WorkflowStep;
import ijfx.ui.main.ImageJFX;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static javafx.application.Application.launch;
import net.imagej.ImageJ;
import net.imagej.ImageJService;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;

public class HistoryListView extends Application {

    @Parameter
    HistoryService historyService;

    @Parameter
    HistoryExecutorService workflowService;

    @Parameter
    ModuleService moduleService;

    @Parameter
    CommandService commandService;

    @Parameter
    Context context;

    @Parameter
    ImageJService imageJService;

    @Override
    public void start(Stage stage) throws Exception {

        ImageJ ij = new ImageJ();

        ij.getContext().inject(this);


        historyService.getStepList().add(
                new DefaultWorkflowStep(context,"net.imagej.plugins.commands.imglib.GaussianBlur")
                
        );

        historyService.getStepList().add(
                new DefaultWorkflowStep(context,"net.imagej.plugins.commands.imglib.GaussianBlur")
               
        );

        historyService.getStepList().add(
                new DefaultWorkflowStep(context,"net.imagej.plugins.commands.imglib.GaussianBlur")
        );

        historyService.getStepList().get(0).setId("the first");
        historyService.getStepList().get(1).setId("the second");
        historyService.getStepList().get(2).setId("the third");

        // birds.forEach(bird -> birdImages.add(new Image(PREFIX + bird + SUFFIX)));
        ListView<WorkflowStep> stepListView = new ListView<>(historyService.getStepList());

      

        //stepListView.setCellFactory(factory);
        stepListView.setPrefWidth(180);

        VBox layout = new VBox(stepListView);
        layout.getStylesheets().add(ImageJFX.getStylesheet());
        layout.setPadding(new Insets(10));

        stage.setScene(new Scene(layout));
        stage.show();
    }

    public static void main(String[] args) {
        launch(HistoryListView.class);
    }

}

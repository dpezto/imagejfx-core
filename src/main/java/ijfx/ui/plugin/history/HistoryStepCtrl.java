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
import ijfx.core.workflow.WorkflowStep;
import ijfx.ui.main.ImageJFX;
import ijfx.ui.loading.LoadingScreenService;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import mongis.utils.FXUtilities;
import javafx.animation.FadeTransition;
import mongis.utils.animation.Animations;
import mongis.utils.task.FluentTask;

/**
 *
 * @author Cyril MONGIS, 2015
 */
public class HistoryStepCtrl extends BorderPane {

    WorkflowStep step;

    @Parameter
    HistoryExecutorService historyExecutorService;

    @Parameter
    HistoryService editService;

    @Parameter
    Context context;

    @Parameter
    LoadingScreenService loadingScreenService;

    @FXML
    Label subtitleLabel;

    @FXML
    Label titleLabel;

    @FXML
    HBox buttonHBox;

    PopOver popover = new PopOver();

    WorkflowStepEditPane moduleConfigPane;

    public HistoryStepCtrl(final Context context) {
        this();
        context.inject(this);
        moduleConfigPane = new WorkflowStepEditPane(context);
        popover.setContentNode(moduleConfigPane);

    }

    private HistoryStepCtrl() {

        try {
            FXUtilities.injectFXML(this);
        } catch (IOException ex) {
            ImageJFX.getLogger().log(Level.SEVERE, null, ex);;
        }

        popover.setAutoHide(true);
        popover.setArrowLocation(PopOver.ArrowLocation.RIGHT_CENTER);
        titleLabel.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onMouseOverTitle);
        // moduleConfigPane.getCloseButton().setOnAction(event -> popover.hide());
        popover.getStyleClass().add("popover");
        addEventHandler(MouseEvent.MOUSE_ENTERED, this::showButtons);
        addEventHandler(MouseEvent.MOUSE_EXITED, this::hideButtons);

        FadeTransition tr = new FadeTransition(Animations.getAnimationDuration(), buttonHBox);
        tr.setToValue(.3);
        tr.setDelay(Duration.millis(3000));
        tr.play();

    }

    public void showButtons(MouseEvent event) {

        FadeTransition tr = new FadeTransition(Animations.getAnimationDuration(), buttonHBox);
        tr.setToValue(1.0);
        tr.play();
        //Animations.FADEIN.configure(buttonHBox, ImageJFX.getAnimationDurationAsDouble()).play();
    }

    public void hideButtons(MouseEvent event) {
        FadeTransition tr = new FadeTransition(Animations.getAnimationDuration(), buttonHBox);
        tr.setToValue(.3);
        tr.play();
    }

    public void onMouseOverTitle(MouseEvent event) {
        edit();
        event.consume();
    }

    public HistoryStepCtrl(WorkflowStep step) {
        this();
        setStep(step);
    }

    public WorkflowStep getStep() {
        return step;
    }

    public void refresh() {
        if(step != null) {
            titleLabel.setText(step.getId());
            subtitleLabel.setText(getSubtitle());
        }
    }
    
    public void setStep(WorkflowStep step) {

        if (this.step == step || step == null) {
            return;
        }

        this.step = step;

        moduleConfigPane.clear();
        moduleConfigPane.edit(step);
        //titleLabel.textProperty().unbind();
       // titleLabel.setText(step.getId());
        
        refresh();
        
        /*
        try {
        moduleConfigPane.configure(step);
        }
        catch(NullPointerException e) {
            try {
                context.inject(step);
            }
            catch(Exception e2) {
                
            }
        }
        //titleLabel.setText(step.getId());
        titleLabel.textProperty().unbind();
        titleLabel.textProperty().bind(moduleConfigPane.getEditableLabel().textProperty());
        subtitleLabel.setText(getSubtitle());

        // titledPane.setContent(this);
        //moduleConfigPane.addEventHandler(ModuleInputEvent.FIELD_CHANGED, this::onFieldChanged);
         */
    }

    private String getSubtitle() {

        StringBuilder builder = new StringBuilder();

        getStep().getParameters().forEach((key, value) -> {
            if (value != null) {

                if (value instanceof File) {
                    value = ((File) value).getName();
                }
                if (value instanceof Boolean) {
                    value = (value.toString().equals("true") ? "yes" : "no");
                }

                builder.append(key + " = " + value);
                builder.append("   ");
            }
        });
        return builder.toString();
    }

    @FXML
    public void delete() {
        editService.getStepList().remove(getStep());
    }

    @FXML
    public void playFrom() {
        editService.playFrom(step);
    }

    @FXML
    public void playUntil() {
        editService.playTo(step);
    }

    @FXML
    public void edit() {
        if (popover.isShowing()) {
            popover.hide();
            return;
        }
        popover.show(titleLabel);

    }

    @FXML
    public void execute() {
        new FluentTask<WorkflowStep, Boolean>()
                .setInput(getStep())
                .callback(historyExecutorService::executeStep)
                .submit(loadingScreenService)
                .start();
    }

    @FXML
    public void executeModule() {
        historyExecutorService.executeModule(getStep());
    }

    @FXML
    public void addToFavorites() {
        editService.getFavoriteList().add(getStep());
    }

    /*
    public void onFieldChanged(ModuleInputEvent event) {
        if (step != null) {
            String id = event.getInput().getName();
            Object value = event.getInput().getValue();
           
            //if (!step.getParameters().get(id).equals(value)) {
                step.getParameters().put(id, value);
                subtitleLabel.setText(getSubtitle());
            //}
        }
    }*/
    @FXML
    public void duplicate() {
        editService.duplicate(step);
    }

    @FXML
    void deleteAll() {
        editService.getStepList().clear();
    }

}

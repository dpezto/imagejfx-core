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
package ijfx.ui.mainwindow;

import ijfx.core.uicontext.ContextualWidget;
import ijfx.core.uicontext.NodeContextualWidget;
import ijfx.ui.UiPluginSorter;

import ijfx.ui.main.ImageJFX;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import java.util.logging.Logger;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import mongis.utils.Handler;
import mongis.utils.animation.Animations;
import mongis.utils.animation.TransitionQueue;
import ijfx.core.uicontext.ContextualContainer;

/**
 *
 * @author Cyril MONGIS, 2015
 */
public class AnimatedPaneContextualView extends HashMap<String, ContextualWidget<Node>> implements ContextualContainer<Node> {

    private String name;

    private Pane pane;

    private Animations animationOnShow = Animations.FADEIN;
    private Animations animationOnHide = Animations.FADEOUT;

    private List<ContextualWidget> toShow;
    private List<ContextualWidget> toHide;

    private Logger logger;

    private int speed = 200;

    private Lock transitionLock = new ReentrantLock();

    private UiPluginSorter<Node> pluginSorter;

    private final TransitionQueue transitionQueue = new TransitionQueue();

    private Handler<ContextualWidget<Node>> onUiPluginDisplayed;

    /**
     *
     * @param sorter Sorter used to sort the nodes after adding or deleting new
     * ones
     * @param name
     * @param node
     */
    public AnimatedPaneContextualView(UiPluginSorter sorter, String name, Pane node) {
        setName(name);
        setPane(node);
        pluginSorter = sorter;
        logger = ImageJFX.getLogger();

    }

    /**
     *
     * @param sorter Sorter used to sort the nodes after update
     * @param node
     */
    public AnimatedPaneContextualView(UiPluginSorter sorter, Pane node) {
        setName(node.getId());
        setPane(node);
        pluginSorter = sorter;
        node.getChildrenUnmodifiable().forEach(child -> registerWidget(new NodeContextualWidget(child)));
        logger = ImageJFX.getLogger();
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     */
    public Pane getPane() {
        return pane;
    }

    /**
     *
     * @param pane
     */
    public void setPane(Pane pane) {
        this.pane = pane;
    }

    /**
     *
     * @return
     */
    public Animations getAnimationOnShow() {
        return animationOnShow;
    }

    /**
     *
     * @param animationOnShow
     * @return
     */
    public AnimatedPaneContextualView setAnimationOnShow(Animations animationOnShow) {
        this.animationOnShow = animationOnShow;
        return this;
    }

    /**
     *
     * @return
     */
    public Animations getAnimationOnHide() {
        return animationOnHide;
    }

    /**
     *
     * @param animationOnHide
     * @return
     */
    public AnimatedPaneContextualView setAnimationOnHide(Animations animationOnHide) {
        this.animationOnHide = animationOnHide;
        return this;
    }

    public void registerWidget(ContextualWidget<Node> widget) {

        put(widget.getName(), widget);

    }

    /**
     *
     * @return
     */
    public int getSpeed() {
        return speed;
    }

    private Lock lock = new ReentrantLock();

    /**
     *
     */
    public void lockPane() {

        
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException ex) {
            ImageJFX.getLogger().log(Level.SEVERE, null, ex);;
        }
    }

    /**
     *
     */
    public void unlockPane() {

        lock.unlock();
    }

    /**
     *
     * @param format
     * @param args
     * @return
     */
    public String msg(String format, Object... args) {
        format = String.format("[%s] ", getName()) + format;
        return String.format(format, args);
    }

    /**
     *
     * @param format
     * @param args
     */
    public void log(String format, Object... args) {
        logger.info(msg(format, args));
    }

    /**
     *
     * @param toShow
     * @param toHide
     * @return
     */
    @Override
    public synchronized ContextualContainer onContextChanged(List<? extends ContextualWidget<Node>> toShow, List<? extends ContextualWidget<Node>> toHide) {

        if (toShow != null && toShow.size() > 0) {
            logger.info(msg(name + " must show %d items : ", toShow.size()));
        }
        toShow.forEach(widget -> logger.info(name + " must show : " + widget.getName()));

        // logging the items to hide
        if (toHide != null && toHide.size() > 0) {
            logger.info(name + " must hide " + toShow.size() + " items : ");
        }
        toHide.forEach(widget -> logger.info("Must hide " + widget.getName()));

        // if nothing must be done, nothing happens
        if (toShow.size() + toHide.size() == 0) {
            return this;
        }

        //creating the appearance and disapearance animations
        Transition disapearance = getAnimationOnHide().configure(getPane(), getSpeed());
        Transition appearance = getAnimationOnShow().configure(getPane(), getSpeed());

        // setting the update of the containers when the pane is not visible
        SequentialTransition sequence = new SequentialTransition(disapearance, appearance);
        disapearance.setOnFinished(actionEvent -> {

            updateContainer(castedList(toShow), castedList(toHide));

        });
        sequence.setOnFinished(event -> {
            toShow.forEach(ctxWidget -> {

                Node node = (Node) ctxWidget.getObject();
                node.fireEvent(new UiContextEvent(UiContextEvent.NODE_DISPLAYED));

                if (onUiPluginDisplayed != null) {

                    onUiPluginDisplayed.handle(ctxWidget);
                }
            });

        });
        // start the sequence by adding it to the animation queue
        log("Start disapearce !");
       //sequence.play();
        transitionQueue.queue(sequence);

        return this;
    }

    /**
     *
     * @param toShow
     * @param toHide
     */
    public void updateContainer(List<NodeContextualWidget> toShow, List<NodeContextualWidget> toHide) {

        Pane pane = getPane();
        pane.setVisible(false);
        List<Node> sortedChildren = new ArrayList<>(pane.getChildren());

        toHide.forEach(widget -> {
            Node node = widget.getNode();
            logger.info("Removing  " + widget);
            sortedChildren.remove(node);
            widget.hide();
        });
        toShow.forEach(widget -> {
            Node node = widget.getNode();

            widget.show();
            logger.info("Adding  " + widget + " to " + pane.getId());
            if (pane.getChildren().contains(node) == false) {
                sortedChildren.add(node);
            }
        });

        //Collection<Node> sortedChilden = pluginSorter.getSortedList(pane.getChildren()); 
        pluginSorter.sort(sortedChildren);
        pane.getChildren().clear();
        pane.getChildren().addAll(sortedChildren);
        pane.setVisible(true);
    }

    /**
     *
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    @Override
    public List<ContextualWidget<Node>> getWidgetList() {
        ArrayList<ContextualWidget<Node>> list = new ArrayList<>();
        values().forEach(widget -> list.add(widget));
        return list;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param origin
     * @return
     */
    public <A, B> ArrayList<B> castedList(List<A> origin) {
        ArrayList<B> newList = new ArrayList<>();
        origin.forEach(o -> newList.add((B) o));
        return newList;
    }

    public AnimatedPaneContextualView setOnUiPluginDisplayed(Handler<ContextualWidget<Node>> onUiPluginDisplayed) {
        this.onUiPluginDisplayed = onUiPluginDisplayed;
        return this;

    }

}

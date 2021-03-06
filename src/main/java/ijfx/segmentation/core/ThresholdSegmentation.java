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
package ijfx.segmentation.core;

import ijfx.commands.binary.SimpleThreshold;
import ijfx.core.image.DisplayRangeService;
import ijfx.core.image.ImagePlaneService;
import ijfx.core.stats.ImageStatisticsService;
import ijfx.core.timer.Timer;
import ijfx.core.timer.TimerService;
import ijfx.core.workflow.Workflow;
import ijfx.core.workflow.WorkflowBuilder;
import ijfx.ui.main.ImageJFX;
import java.util.Map;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mongis.utils.task.FluentTask;
import mongis.utils.TimedBuffer;
import net.imagej.autoscale.AutoscaleService;
import net.imagej.autoscale.DataRange;
import net.imagej.display.ImageDisplayService;
import net.imagej.threshold.ThresholdMethod;
import net.imagej.threshold.ThresholdService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

/**
 *
 * @author Cyril MONGIS
 */
public class ThresholdSegmentation extends AbstractSegmentation {

    // ** min max representing the model
    public final DoubleProperty lowValue = new SimpleDoubleProperty();
    public final DoubleProperty highValue = new SimpleDoubleProperty();
    public final DoubleProperty minValue = new SimpleDoubleProperty();
    public final DoubleProperty maxValue = new SimpleDoubleProperty();

    

    private final Property<Img<BitType>> maskProperty = new SimpleObjectProperty();

    private final BooleanProperty activatedProperty = new SimpleBooleanProperty();

    public final StringProperty methodProperty = new SimpleStringProperty("Default");

    public boolean hasChanged = true;
    
    public BooleanProperty autoThreshold = new SimpleBooleanProperty();

    Logger logger = ImageJFX.getLogger();

    Map<String, ThresholdMethod> thresholdMethods;

    /*
        SciJava classes
     */
    @Parameter
    private DisplayRangeService displayRangeService;

    @Parameter
    private ImagePlaneService imagePlaneService;

    @Parameter
    private ThresholdService thresholdService;

    @Parameter
    private ImageDisplayService imageDisplayService;

    @Parameter
    private ImageStatisticsService imageStatisticsService;


    @Parameter
    private AutoscaleService autoScaleService;

    @Parameter
    private Context context;

    private RandomAccessibleInterval<? extends RealType<?>> plane;
    
    TimedBuffer<Runnable> maskUpdateBuffer = new TimedBuffer(100);
    
    private boolean initiated = false;
    
    @Parameter
    TimerService timerService;
    
    private static Timer timer;
    
    public ThresholdSegmentation() {
      
       
       
     
       
        
    }
    
    private Timer getTimer() {
        if(timer == null) {
            timer = timerService.getTimer(this.getClass());
        }
        return timer;
    }
    
    private void restartTimer() {
        getTimer().start();
    }
    private void elapsed(String msg) {
        getTimer().elapsed(msg);
    }
    
     public <T extends RealType<?>> void preview(RandomAccessibleInterval<T> input) {
         
         initListeners();
         
         if(plane != input && input != null) {

             double[] minMax = imageStatisticsService.getMinMax((RandomAccessibleInterval)input);
             
             double min = minMax[0];
             double max = minMax[1];
             
             if(min > lowValue.get()) {
                 min = lowValue.get();
             }
             if(max < highValue.get()) {
                 max = highValue.get();
             }
             
             minValue.setValue(min);
             maxValue.setValue(max);
             hasChanged = true;
         }
         
         this.plane = input;
         
         
         
         requestMaskUpdate();
     }

    @Override
    public Workflow getWorkflow() {
        return new WorkflowBuilder(context)
                .addStep(SimpleThreshold.class, "value", lowValue.getValue(), "makeBinary", true, "upperCut", false)
                .getWorkflow("Simple threshold");
    }

    @Override
    public Property<Img<BitType>> maskProperty() {
        return maskProperty;
    }

    /*
        Getters and setters
     */
    private boolean isAutothreshold() {
        return autoThreshold.getValue();
    }

    /*
         Listeners
     */
    private void initListeners() {

        if(thresholdMethods != null) return;
        
        // setting the autothreshold status to true
        autoThreshold.setValue(Boolean.TRUE);

        // listening the min and max values for updating the max
        lowValue.addListener(this::onAnyChange);
        highValue.addListener(this::onAnyChange);

        // listenening for the mode -> should calculate threshold values
        autoThreshold.addListener(this::onAnyChange);

        // a mask update should be only occurs every 50ms
        maskUpdateBuffer.setAction(list -> updateMask());

        // filling the threshold methods
        thresholdMethods = thresholdService.getThresholdMethods();

        methodProperty.addListener(this::onAnyChange);
        
        
        
        
    }

    

    private void onAutothresholdChanged(Observable obs, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            calculateThresholdValues((RandomAccessibleInterval) plane);
        }
    }

    /*
    @Override
    public void preview(FXImageDisplay imageDisplay) {
        if(!initiated) {
            initListeners();
        }
        setImageDisplay(imageDisplay);
        requestMaskUpdate();
    }*/

   

    private void onAnyChange(Observable obs, Object oldValue, Object newValue) {
        logger.info("min max request !");
        hasChanged = true;
        Platform.runLater(this::requestMaskUpdate);
    }


    private void requestMaskUpdate() {
        maskUpdateBuffer.add(this::updateMask);
    }
    
    @Override
    public void refresh() {
        hasChanged = true;
        requestMaskUpdate();
    }

    private void updateMask() {

        new FluentTask<Void, Img<BitType>>()
                .call(this::generateMask)
                .then(maskProperty::setValue)
                .start();

    }
    /*
    private <T extends RealType<T>> IntervalView<T> getAccessInterval(FXImageDisplay imageDisplay) {

        Dataset dataset = imageDisplayService.getActiveDataset(imageDisplay);

        // localizing
        long[] position = new long[imageDisplay.numDimensions()];
        DatasetView activeDatasetView = imageDisplayService.getActiveDatasetView(imageDisplay);
        activeDatasetView.localize(position);

        IntervalView<T> planeView = imagePlaneService.planeView(dataset, DimensionUtils.absoluteToPlanar(position));

        return planeView;

    }*/

    private <T extends RealType<T>> double[] calculateThresholdValues(RandomAccessibleInterval<T> plane) {

       

        if (plane == null) {
            return null;
        }

        logger.info("Calculating the threshold values");

        IterableInterval<T> planeView = Views.iterable(plane);

        String methodStr = methodProperty.getValue();

        ThresholdMethod method = thresholdMethods.get(methodStr);

        DataRange range = autoScaleService.getDefaultIntervalRange(planeView);
         
        Histogram1d<T> histogram = new Histogram1d<T>(planeView, new Real1dBinMapper<T>(range.getMin(), range.getMax(), getBins(range), true));

        long threshold = method.getThreshold(histogram);
        T val = histogram.firstDataValue().createVariable();
        
        histogram.getLowerBound(threshold, val);

        double min = val.getRealDouble();
        double max = range.getMax();

        if(histogram.firstDataValue().getMinIncrement() == 1) {
            min = Math.floor(min);
            max = Math.floor(max);
        }
        
        return new double[]{min, max};

    }

    private  <T extends RealType<T>> Img<BitType> generateMask() {

      
        restartTimer();
        if(hasChanged == false) return maskProperty.getValue();
        
        logger.info("Generating mask !");
      
        ImgFactory<BitType> factory = new ArrayImgFactory<>();
        long[] dimension = new long[2];
        plane.dimensions(dimension);

        Img<BitType> img = factory.create(dimension, new BitType());

        elapsed("creating new image");
        
        final double min, max;

        if (isAutothreshold()) {
            
            
            final double[] minMax  = calculateThresholdValues((RandomAccessibleInterval) plane);
            elapsed("autothreshold");
            min = minMax[0];
            max = minMax[1];
        } else {
            min = lowValue.get();
            max = highValue.get();
        }
        double value;
        IterableInterval<? extends RealType<?>> iterable = Views.iterable(plane);
        Cursor<? extends RealType<?>> cursor = iterable.cursor();
        RandomAccess<BitType> randomAccess = img.randomAccess();
        cursor.reset();
        while (cursor.hasNext()) {
            cursor.fwd();
            value = cursor.get().getRealDouble();
            randomAccess.setPosition(cursor);
            randomAccess.get().set(value >= min && value <= max);
        }
        elapsed("setting pixels");
        if(lowValue.get() != min)lowValue.setValue(min);
        if(highValue.get() != max) highValue.setValue(max);

        hasChanged = false;
        
        return img;

    }

    private int getBins(DataRange range) {

        if (range.getExtent() > 16000) {
            return 16000;
        } else {
            return new Double(range.getExtent()).intValue();
        }

    }

}

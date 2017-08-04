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
package ijfx.core.segmentation;

import com.google.common.collect.Lists;
import ijfx.commands.binary.BinaryToOverlay;
import ijfx.core.batch.item.BatchItemBuilder;
import ijfx.core.image.ImagePlaneService;
import ijfx.core.metadata.MetaDataService;
import ijfx.core.metadata.MetaDataSet;
import ijfx.core.metadata.MetaDataSetType;
import ijfx.core.overlay.MeasurementService;
import ijfx.core.overlay.OverlayUtilsService;
import ijfx.core.utils.AxisUtils;
import ijfx.core.utils.DimensionUtils;
import ijfx.core.workflow.DefaultWorkflow;
import ijfx.core.workflow.Workflow;
import ijfx.core.workflow.WorkflowStep;
import ijfx.ui.main.ImageJFX;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mongis.utils.ProgressHandler;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.overlay.Overlay;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

/**
 *
 * @author cyril
 */
public class SegmentationTaskBuilder {

    boolean separatePlane = true;

    final BatchItemBuilder itemBuilder;

    Workflow workflow;

    Logger logger = ImageJFX.getLogger();

    @Parameter
    Context context;

    @Parameter
    MeasurementService measurementService;

    @Parameter
    ImagePlaneService imagePlaneService;

    @Parameter
    MetaDataService metaDataService;

    @Parameter
    OverlayUtilsService overlayUtilsService;

    @Parameter
    DatasetService datasetService;

    @Parameter
    ImageDisplayService imageDisplayService;

    List<SegmentationOp> opList = new ArrayList<>();

    public SegmentationTaskBuilder(Context context) {
        context.inject(this);
        itemBuilder = new BatchItemBuilder(context);

    }

    /**
     * This function will retrieve any BitMask from the image display and use it
     * as a mask for each plane. If no mask is given, the workflow will then be
     * executed on the current image
     *
     * @param display
     * @return Builder
     */
    public SegmentationTaskBuilder addImageDisplay(ImageDisplay display) {

        Img<BitType> mask = overlayUtilsService.extractBinaryMask(display);
        
        Dataset dataset = imageDisplayService.getActiveDataset(display);
        
        if (mask == null) {
            Dataset maskDataset = datasetService.create((RandomAccessibleInterval) imagePlaneService.planeView(display));
            
            
            
            opList.add(new DefaultSegmentationTask(imageDisplayService.getActiveDataset(display), maskDataset, workflow, null));
            
            
        }
        else {
            opList.add(new DefaultSegmentationTask(dataset, mask, workflow, null));
        }

        return this;
    }

    public SegmentationTaskBuilder addDataset(Dataset dataset, MetaDataSet set, boolean separatePlane) {

        List<SegmentationOp> ops = new ArrayList<>();

        if (separatePlane = false || dataset.numDimensions() == 2) {
            ops = Lists.newArrayList(new DefaultSegmentationTask(dataset, workflow, set));
        } else {
            long[][] possibilities = DimensionUtils.allPossibilities(dataset);
            ops = Stream
                    .of(possibilities)
                    .map(position -> imagePlaneService.isolatePlane(dataset, DimensionUtils.planarToAbsolute(position)))
                    .map(plane -> new DefaultSegmentationTask(plane, workflow, set))
                    .collect(Collectors.toList());
        }
        return this;
    }

    public SegmentationTaskBuilder addInterval(RandomAccessibleInterval<?> interval) {
        itemBuilder.from(interval);
        return this;
    }

    public SegmentationTaskBuilder setWorkflow(List<WorkflowStep> steps) {
        return setWorkflow(new DefaultWorkflow(steps));
    }

    public SegmentationTaskBuilder setWorkflow(Workflow workflow) {
        this.workflow = workflow;
        return this;
    }

    private SegmentationTaskBuilder separatePlanes() {
        separatePlane = true;
        return this;
    }

    private <T> SegmentationOpList<T> build(SegmentationHandler<T> handler) {

        SegmentationOpList<T> segmentationOpList = new SegmentationOpList<>(handler);
        context.inject(segmentationOpList);
        segmentationOpList.addAll(opList);

        return segmentationOpList;
        // take input dataset   
    }

    public SegmentationOpList<List<? extends SegmentedObject>> measure() {
        return build(this::measure);
    }

    public SegmentationOpList<Img<BitType>> getAsMask() {
        return build(this::getAsMask);
    }

    private Img<BitType> getAsMask(ProgressHandler handler, MetaDataSet set, Dataset original, Img<BitType> mask) {
        return mask;
    }

    private List<? extends SegmentedObject> measure(ProgressHandler handler, MetaDataSet set, Dataset original, Img<BitType> mask) {

        List<Overlay> overlays = Lists.newArrayList(BinaryToOverlay.transform(context, mask, true));

        handler.setTotal(overlays.size());

        List<SegmentedObject> objects = new ArrayList<SegmentedObject>();

        if (original.numDimensions() > 2) {

            CalibratedAxis[] axes = AxisUtils.getAxes(original);

            for (long[] position : DimensionUtils.allPossibilities(original)) {
                MetaDataSet planeMetaDataSet = new MetaDataSet(MetaDataSetType.PLANE);
                planeMetaDataSet.merge(set);

                metaDataService.fillPositionMetaData(planeMetaDataSet, axes, position);

                List<? extends SegmentedObject> measureOverlays = measurementService.measureOverlays(overlays, original, position);

                measureOverlays.forEach(obj -> obj.getMetaDataSet().merge(planeMetaDataSet));

                handler.increment(1);
                objects.addAll(measureOverlays);

            }
        } else {
            List<SegmentedObject> measureOverlays = measurementService.measureOverlays(overlays, (RandomAccessibleInterval) original);
            measureOverlays
                    .forEach(obj -> obj.getMetaDataSet().merge(set));

            objects.addAll(measureOverlays);
        }

        return objects;
    }
}

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
package ijfx.core.image;

import ijfx.core.image.sampler.DatasetSamplerService;
import ijfx.core.metadata.MetaData;
import ijfx.core.metadata.MetaDataOwner;
import ijfx.core.stats.ImageStatisticsService;
import ijfx.core.timer.Timer;
import ijfx.core.timer.TimerService;
import ijfx.ui.main.ImageJFX;
import io.scif.MetadataLevel;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import mongis.utils.uuidmap.UUIDWeakHashMap;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imagej.plugins.commands.assign.DivideDataValuesBy;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable8;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.apache.commons.io.FilenameUtils;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.module.MethodCallException;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 *
 * @author Tuan anh TRINH
 */
@Plugin(type = Service.class)
public class DefaultDatasetUtilsService extends AbstractService implements DatasetUtilsService {

    @Parameter
    private DatasetSamplerService datasetSamplerService;

    @Parameter
    private ImageDisplayService imageDisplayService;

    @Parameter
    private CommandService commandService;

    @Parameter
    private ModuleService moduleService;

    @Parameter
    private DisplayService displayService;

    @Parameter
    private DatasetService datasetService;

    @Parameter
    private ImageStatisticsService imageStatisticsService;

    @Parameter
    private TimerService timerService;

    @Parameter
    private ImagePlaneService imagePlaneService;

    @Parameter
    private OpService opService;

    private final Logger logger = ImageJFX.getLogger();

    public final static String DEFAULT_SEPARATOR = " - ";

    private UUIDWeakHashMap<Dataset> virtualDatasetMap = new UUIDWeakHashMap<>();

    @Override
    public Dataset extractPlane(ImageDisplay imageDisplay) throws NullPointerException {
        CalibratedAxis[] calibratedAxises = new CalibratedAxis[imageDisplay.numDimensions()];
        int[] position = new int[imageDisplay.numDimensions()];
        imageDisplay.localize(position);
        Dataset dataset = (Dataset) imageDisplay.getActiveView().getData();
        imageDisplay.axes(calibratedAxises);
        for (int i = 2; i < position.length; i++) {
            dataset = datasetSamplerService.isolateDimension(dataset, calibratedAxises[i].type(), position[i]);
        }
        return dataset;
    }

    @Override
    public ImageDisplay getImageDisplay(Dataset dataset) {
        return imageDisplayService.getImageDisplays()
                .parallelStream()
                .filter((d) -> imageDisplayService.getActiveDataset(d) == dataset)
                .findFirst().orElse(null);
    }

    /**
     * Divide 2 different Dataset with different dimensions
     *
     * @param <T>
     * @param numerator
     * @param denominator
     * @return
     */
    @Override
    public < T extends RealType< T>> Dataset divideDatasetByDataset(Dataset numerator, Dataset denominator) {

        Dataset resultDataset = numerator.duplicateBlank();

        RandomAccess<T> resultRandomAccess = (RandomAccess<T>) resultDataset.randomAccess();
        Cursor<T> numeratorCursor = (Cursor<T>) numerator.cursor();
        RandomAccess<T> denominatorRandomAccess = (RandomAccess<T>) denominator.randomAccess();

        int[] positionDenominator = new int[denominator.numDimensions()];
        denominatorRandomAccess.localize(positionDenominator);
        while (numeratorCursor.hasNext()) {
            numeratorCursor.next();
            //Set position
            positionDenominator[0] = numeratorCursor.getIntPosition(0);
            positionDenominator[1] = numeratorCursor.getIntPosition(1);
            denominatorRandomAccess.setPosition(positionDenominator);
            resultRandomAccess.setPosition(numeratorCursor);

            //Calculate value
            try {

                resultRandomAccess.get().set(numeratorCursor.get());
            } catch (Exception e) {
                ImageJFX.getLogger().log(Level.SEVERE,null,e);
            }
            Float f = numeratorCursor.get().getRealFloat() / denominatorRandomAccess.get().getRealFloat();
            resultRandomAccess.get().setReal(f);

        }
        return resultDataset;
    }

    @Override
    public Dataset divideDatasetByValue(Dataset dataset, double value) {
        ImageDisplay display = this.getImageDisplay(dataset);
        if (display == null) {
            display = (ImageDisplay) displayService.createDisplay(dataset.getName(), dataset);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("value", value);
        parameters.put("preview", false);
        parameters.put("allPlanes", true);
        parameters.put("display", display);
        Module module = executeCommand(DivideDataValuesBy.class, parameters);
        ImageDisplay imageDisplay = (ImageDisplay) module.getOutput("display");
        Dataset datasetResult = (Dataset) imageDisplay.getActiveView().getData();
        return datasetResult;

    }

    //Not here... But where?
    public <C extends Command> Module executeCommand(Class<C> type, Map<String, Object> parameters) {
        Module module = moduleService.createModule(commandService.getCommand(type));
        try {
            module.initialize();
        } catch (MethodCallException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        parameters.forEach((k, v) -> {
            module.setInput(k, v);
            module.setResolved(k, true);
        });

        Future run = moduleService.run(module, false, parameters);

        try {
            run.get();
        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return module;
    }

    @Override
    public Dataset divideActivePlaneByValue(Dataset dataset, long[] position, double value) {
        double width = dataset.max(0) + 1;
        double height = dataset.max(1) + 1;

        RandomAccess<RealType<?>> randomAccess = dataset.randomAccess();
        randomAccess.setPosition(position);
        for (int x = 0; x < width; x++) {
            randomAccess.setPosition(x, 0);
            for (int y = 0; y < height; y++) {
                randomAccess.setPosition(y, 1);
                Double d = (Double) randomAccess.get().getRealDouble() / value;
                randomAccess.get().setReal(d);
            }
        }
        return dataset;
    }

    @Override
    public Dataset divideActivePlaneByActivePlane(Dataset dataset, long[] position, Dataset datasetValue, long[] positionValue) {
//        long[] position = new long[dataset.numDimensions()];
//        this.getImageDisplay(dataset).localize(position);
        double width = dataset.max(0) + 1;
        double height = dataset.max(1) + 1;

//        long[] positionValue = new long[datasetValue.numDimensions()];
//        this.getImageDisplay(dataset).localize(positionValue);
        double widthValue = datasetValue.max(0) + 1;
        double heightValue = datasetValue.max(1) + 1;

        if (width != widthValue || height != heightValue) {
            throw new IllegalArgumentException("The sizes have to be the same");
        }
        RandomAccess<RealType<?>> randomAccess = dataset.randomAccess();
        RandomAccess<RealType<?>> randomAccessValue = datasetValue.randomAccess();

        randomAccess.setPosition(position);
        randomAccessValue.setPosition(positionValue);
        for (int x = 0; x < width; x++) {
            randomAccess.setPosition(x, 0);
            randomAccessValue.setPosition(x, 0);
            for (int y = 0; y < height; y++) {
                randomAccess.setPosition(y, 1);
                randomAccessValue.setPosition(y, 1);
//                System.out.println("ijfx.service.dataset.DefaultDatasetUtillsService.divideActivePlaneByActivePlane()");
//                System.out.println(randomAccess.get().getRealDouble());
                Double d = randomAccess.get().getRealDouble() / randomAccessValue.get().getRealDouble();
                randomAccess.get().setReal(d);
//                System.out.println(randomAccess.get().getRealDouble());
            }
        }
        return dataset;
    }

    @Override
    public <T extends RealType<T> & NativeType<T>> Dataset emptyConversion(Dataset dataset, T t) {

        long[] dimensions = new long[dataset.numDimensions()];
        AxisType[] axisTypes = new AxisType[dataset.numDimensions()];
        CalibratedAxis[] axes = new CalibratedAxis[dataset.numDimensions()];
        dataset.axes(axes);

        Dataset output = datasetService.create(t, dimensions, "", axisTypes);
        output.setAxes(axes);
        output.setName(dataset.getName());
        output.setSource(dataset.getSource());

        return output;

    }

    private void copyMetaData(Dataset source, Dataset target) {
        CalibratedAxis[] axes = new CalibratedAxis[source.numDimensions()];
        source.axes(axes);

        target.setAxes(axes);
        target.setName(target.getName());
        target.setSource(target.getSource());

    }

    @Override
    public <T extends RealType<T> & NativeType<T>> Dataset convert(Dataset dataset, T t) {
        long[] dimensions = new long[dataset.numDimensions()];
        AxisType[] axisTypes = new AxisType[dataset.numDimensions()];

        dataset.dimensions(dimensions);

        for (int i = 0; i != dimensions.length; i++) {
            axisTypes[i] = dataset.axis(i).type();
        }

        Dataset output = datasetService.create(t, dimensions, "", axisTypes);

        Cursor<? extends RealType<?>> cursor = dataset.cursor();
        cursor.reset();
        RandomAccess<RealType<?>> randomAccess = output.randomAccess();
        while (cursor.hasNext()) {
            cursor.fwd();

            randomAccess.setPosition(cursor);
            randomAccess.get().setReal(cursor.get().getRealDouble());

        }

        copyMetaData(dataset, output);
        return output;
    }

    public void addSuffix(Dataset dataset, String suffix, String separator) {
        if (separator == null) {
            separator = DEFAULT_SEPARATOR;
        }

        String datasetName = dataset.getName();
        File datasetFolder;
        if (dataset.getSource() != null) {
            datasetFolder = new File(dataset.getSource()).getParentFile();
        } else {
            datasetFolder = new File("./");
        }

        String baseName = FilenameUtils.getBaseName(datasetName);
        String extension = FilenameUtils.getExtension(datasetName);

        dataset.setName(
                new StringBuilder()
                        .append(baseName)
                        .append(separator)
                        .append(suffix)
                        .append(".")
                        .append(extension)
                        .toString());

        dataset.setSource(new File(datasetFolder, dataset.getName()).getAbsolutePath());

    }

    public Dataset open(File file, int imageId, boolean virtual) throws IOException {

        Timer timer = timerService.getTimer(this.getClass());
        Dataset dataset = null;
        SCIFIOConfig config = new SCIFIOConfig();

        if (virtual && virtualDatasetMap.key(file.getAbsolutePath(), imageId).has()) {
            return virtualDatasetMap.key(file.getAbsolutePath(), imageId).get();
        }

        config.parserSetLevel(MetadataLevel.MINIMUM);

        if (virtual) {
            config.imgOpenerSetImgModes(SCIFIOConfig.ImgMode.CELL);
            config.imgOpenerSetImgFactoryHeuristic(new CellImgFactoryHeuristic());

        }

        config.imgOpenerSetIndex(imageId);
        config.imgOpenerSetComputeMinMax(false);
        config.imgOpenerSetOpenAllImages(false);
        config.groupableSetGroupFiles(false);
        
        final ImgOpener imageOpener = new ImgOpener(getContext());

        try {
            final SCIFIOImgPlus<?> imgPlus
                    = imageOpener.openImgs(file.getAbsolutePath(), config).get(0);

            dataset = datasetService.create((ImgPlus) imgPlus);

        } catch (Exception e) {
            ImageJFX.getLogger().log(Level.SEVERE, "Error when opening " + file.getName(), e);
            throw new IOException();
        }

        if (virtual && dataset != null) {
            virtualDatasetMap.key(file.getAbsolutePath(), imageId).put(dataset);
        }
        //Dataset dataset = datasetIOService.open(file.getAbsolutePath(), config);
        timer.elapsed(String.format("Dataset opening (%s) (virtual = %s)", file.getName(), virtual));
        return dataset;
    }

    public void copyInfos(Dataset source, Dataset target) {

        target.setName(source.getName());

        target.setSource(source.getName());

        for (int i = 0; i != target.numDimensions(); i++) {

            CalibratedAxis sAxis = source.axis(i);
            target.setAxis(sAxis, i);

        }
    }

    public <T extends RealType<T>> Image datasetToImage(RandomAccessibleInterval<T> dataset, ColorTable colorTable) {

        double[] minMax = imageStatisticsService.getMinMax(dataset);
        double min = minMax[0];
        double max = minMax[1];
        double range = max - min;
        min += range * 0.1;
        max -= range * 0.1;
        //SummaryStatistics summaryStatistics = ijfxStatsService.getSummaryStatistics(Views.iterable(dataset).cursor());

        return datasetToImage(dataset, colorTable, min, max);

    }

    public <T extends RealType<T>> Image datasetToImage(RandomAccessibleInterval<T> dataset) {
        return datasetToImage(dataset, new ColorTable8());
    }

    public <T extends RealType<T>> Image datasetToImage(RandomAccessibleInterval<T> dataset, ColorTable colorTable, double min, double max, WritableImage image) {

        //int width = (int) dataset.dimension(0);
        //int height = (int) dataset.dimension(1);
        //WritableImage image = new WritableImage(width, height);
        RealLUTConverter<T> converter = new RealLUTConverter<T>(min, max, colorTable);
        ARGBType argb = new ARGBType();
        RandomAccess<T> ra = dataset.randomAccess();

        Cursor<T> cursor = Views.iterable(dataset).cursor();
        cursor.reset();
        int[] position = new int[dataset.numDimensions()];
        while (cursor.hasNext()) {
            cursor.fwd();
            cursor.localize(position);
            converter.convert(cursor.get(), argb);
            image.getPixelWriter().setArgb(position[0], position[1], argb.get());
        }

        return image;
    }

    public <T extends RealType<T>> Image datasetToImage(RandomAccessibleInterval<T> dataset, ColorTable colorTable, double min, double max) {

        int width = (int) dataset.dimension(0);
        int height = (int) dataset.dimension(1);
        WritableImage image = new WritableImage(width, height);

        datasetToImage(dataset, colorTable, min, max, image);

        return image;
    }

    @Override
    public Dataset openSource(MetaDataOwner explorable, boolean virtual) throws IOException {
        String source = explorable.getMetaDataSet().get(MetaData.SOURCE_PATH).getStringValue();
        if (source == null || "null".equals(source)) {
            source = explorable.getMetaData(MetaData.ABSOLUTE_PATH).getStringValue();
        }

        if (source == null) {
            throw new IllegalArgumentException("The explorable has no source");
        }

        Integer serie = explorable
                .getMetaDataSet()
                .getOrDefault(MetaData.SERIE, MetaData.create(MetaData.SERIE, 0))
                .getIntegerValue();

        Dataset dataset = open(new File(source), serie, virtual);

        return dataset;
    }

    @Override
    public Dataset copy(Dataset dataset) {

        Dataset result = copyDataset(dataset);
        copyInfos(dataset, result);
        return result;
    }

    private <T extends RealType<T>> Dataset copyDataset(Dataset dataset) {
        try {
            return datasetService.create((ImgPlus<T>) dataset.getImgPlus().copy());
        } catch (NullPointerException e) {
            Dataset copy = imagePlaneService.createEmptyPlaneDataset(dataset);

            IterableInterval<T> iterable = Views.iterable((RandomAccessibleInterval<T>) dataset);

            Cursor<T> cursor = iterable.cursor();
            RandomAccess<T> out = (RandomAccess<T>) copy.randomAccess();
            cursor.reset();
            while (cursor.hasNext()) {
                cursor.fwd();
                out.setPosition(cursor);
                out.get().set(cursor.get());
            }

            return copy;

        }
    }

}

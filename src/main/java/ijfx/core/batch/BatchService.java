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
package ijfx.core.batch;

import ijfx.core.image.SilentImageDisplay;
import ijfx.core.postprocessor.DatasetArrayPostprocessor;
import ijfx.core.timer.Timer;
import ijfx.core.timer.TimerService;
import ijfx.core.workflow.Workflow;
import ijfx.core.workflow.WorkflowRecorderPreprocessor;
import ijfx.core.workflow.WorkflowStep;
import ijfx.ui.inputharvesting.InputHarversterFX;
import ijfx.ui.main.ImageJFX;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import mongis.utils.task.FluentTask;
import mongis.utils.task.ProgressHandler;
import mongis.utils.task.SilentProgressHandler;
import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.process.ActiveDataViewPreprocessor;
import net.imagej.display.process.ActiveDatasetPreprocessor;
import net.imagej.display.process.ActiveDatasetViewPreprocessor;
import net.imagej.display.process.ActiveImageDisplayPreprocessor;
import org.apache.commons.lang3.ArrayUtils;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.display.ActiveDisplayPreprocessor;
import org.scijava.display.DisplayPostprocessor;
import org.scijava.display.DisplayService;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;
import org.scijava.module.process.InitPreprocessor;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.module.process.SaveInputsPreprocessor;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 *
 * @author Cyril MONGIS, 2015
 */
@Plugin(type = Service.class)
public class BatchService extends AbstractService implements ImageJService {

    @Parameter
    private ModuleService moduleService;

    @Parameter
    private CommandService commandService;

    private Logger logger = ImageJFX.getLogger();

    @Parameter
    private ImageDisplayService imageDisplayService;

    @Parameter
    private DisplayService displayService;

    @Parameter
    private PluginService pluginService;

    @Parameter
    private TimerService timerService;

    private boolean running = false;

    private final ArrayList<Class<?>> processorBlackList = new ArrayList<>();

    public final Class<?>[] BLACKLIST = {
        DisplayPostprocessor.class,
        InitPreprocessor.class,
        WorkflowRecorderPreprocessor.class,
        DatasetArrayPostprocessor.class,
        InputHarversterFX.class,
        SaveInputsPreprocessor.class
    };

    public BatchService() {
        super();
        Stream
                .of(BLACKLIST)
                .forEach(processorBlackList::add);

    }

    // applies a single modules to multiple inputs and save them
    public Boolean applyModule(ProgressHandler progress, List<BatchSingleInput> inputs, final Module module, boolean process, HashMap<String, Object> parameters) {

        int totalOps = inputs.size();
        int count = 0;

        for (BatchSingleInput input : inputs) {
            input.load();
            count++;
            final Module createdModule = moduleService.createModule(module.getInfo());
            if (!executeModule(input, createdModule, parameters)) {
                return false;
            }
            input.save();
            progress.setProgress(count, totalOps);
        }

        return true;

    }

    public Task<Boolean> applyWorkflow(List<BatchSingleInput> inputs, Workflow workflow) {
        return new FluentTask<List<BatchSingleInput>, Boolean>()
                .setInput(inputs)
                .callback((progress, input) -> applyWorkflow(progress, inputs, workflow));
    }

    public Boolean applyWorkflow(ProgressHandler handler, BatchSingleInput input, Workflow workflow) {
        List<BatchSingleInput> inputList = new ArrayList<>();
        inputList.add(input);
        return applyWorkflow(handler, inputList, workflow);
    }

    // applies a workflow to a list of inputs
    public Boolean applyWorkflow(ProgressHandler progress, List<? extends BatchSingleInput> inputs, Workflow workflow) {

        final Timer t = timerService.getTimer("Workflow");

        if (progress == null) {
            progress = new SilentProgressHandler();
        }

        Boolean lock = new Boolean(true);

        if (workflow == null) {
            logger.warning("No workflow was provided");
            return true;
        }

        int totalOps = inputs.size() * (2 + workflow.getStepList().size());

        progress.setStatus("Starting batch processing...");

        boolean success = true;
        BooleanProperty successProperty = new SimpleBooleanProperty();
        Exception error = null;
        setRunning(true);

        BiConsumer<String, String> logTime = (step, msg) -> {
            t.elapsed(String.format("[%s][%s]%s", workflow.getName(), step, msg));
        };

        progress.setTotal(totalOps);

        for (int i = 0; i != inputs.size(); i++) {
            //inputs.parallelStream().forEach(input->{
            logger.info("Running...");

            final BatchSingleInput input = inputs.get(i);

            if (progress.isCancelled()) {
                progress.setStatus("Batch Processing cancelled");
                success = false;
                //return;
                break;

            }

            t.start();
            synchronized (lock) {
                logger.info("Loading input...");
                progress.setStatus("Loading %s...", input.getName());
                try {
                    getContext().inject(input);
                } catch (IllegalStateException ise) {
                    logger.warning("Context already injected");
                }
                try {
                    input.load();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Couldn't load input", e);
                    error = e;
                    success = false;
                    break;

                }
                logger.info("Input loaded");
            }
            logTime.accept("loading", "done");
            progress.increment(1);
            if (i < inputs.size() - 1) {
                // loading the next one while processing the current one
                BatchSingleInput next = inputs.get(i + 1);
                ImageJFX.getThreadPool().execute(() -> {
                    synchronized (lock) {
                        logger.info("Loading next input...");
                        next.load();
                        logger.info("Next input loaded.");

                    }
                });
            }

            for (WorkflowStep step : workflow.getStepList()) {
                logger.info("Executing step : " + step.getId());
                String title;
                try {
                    title = step.getModule().getInfo().getTitle();
                    progress.setStatus(String.format("Processing %s with %s", input.getName(), title));

                } catch (NullPointerException e) {
                    title = "???";
                    progress.setStatus("...");
                }
                progress.increment(1);

                final Module module = moduleService.createModule(step.getModule().getInfo());
                try {
                    getContext().inject(module.getDelegateObject());
                } catch (Exception e) {
                    logger.warning("Context already injected in module ?");
                }
                logTime.accept("injection", "done");
                logger.info("Module created : " + module.getDelegateObject().getClass().getSimpleName());
                if (!executeModule(input, module, step.getParameters())) {

                    progress.setStatus("Error :-(");
                    progress.setProgress(0, 1);
                    success = false;
                    logger.info("Error when executing module : " + module.getInfo().getName());
                    break;
                };
                logTime.accept(title, "done");

            }

            if (success == false) {
                break;
            }

            synchronized (lock) {
                progress.setStatus("Saving %s...", input.getName());
                input.save();
                progress.increment(1);
            }
            logTime.accept("saving", "done");
            input.dispose();
        }

        if (success) {
            logger.info("Batch processing completed");
            progress.setStatus("Batch processing completed.");
            progress.setProgress(1.0);

        } else if (progress.isCancelled()) {
            progress.setStatus("Batch processing cancelled");
        } else {

            progress.setStatus("An error happend during the process.");
            progress.setProgress(1, 1);
        }
        setRunning(false);
        return success;

    }
    // execute a module (with all the side parameters injected)

    public boolean executeModule(BatchSingleInput input, Module module, Map<String, Object> parameters) {

        logger.info("Executing module " + module.getDelegateObject().getClass().getSimpleName());
        logger.info("Injecting input");
        boolean inputInjectionSuccess = injectInput(input, module);

        if (!inputInjectionSuccess) {
            logger.warning("Error when injecting input.");
            return false;
        }

        logger.info("Injecting parameters");
        parameters.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            logger.info(String.format("Parameter : %s = %s", key, value.toString()));
            module.setInput(key, value);
            module.setResolved(key, true);
        });

        module.getInputs().forEach((key, value) -> {
            module.setResolved(key, true);
        });

        // calling the batch preprocessor plugins
        pluginService
                .createInstancesOfType(BatchPrepreprocessorPlugin.class)
                .forEach(processor -> processor.process(input, module, parameters));

        String moduleName = module.getInfo().getDelegateClassName();
        logger.info(String.format("[%s] starting module", moduleName));

        logger.info("Running module");
        Future<Module> run;

        try {
            //getContext().inject(run);
            getContext().inject(module);
            module.initialize();
        } catch (Exception e) {
            logger.info("Context already injected.");
            //   ImageJFX.getLogger().log(Level.SEVERE,null,e);
        }
        run = moduleService.run(module, getPreProcessors(), getPostprocessors(), parameters);
        // } else {
        //     run = moduleService.run(module, process, parameters);

        //}
        logger.info(String.format("[%s] module started", moduleName));

        try {
            run.get();
            logger.info(String.format("[%s] module finished", moduleName));
            extractOutput(input, module);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error when extracting ouput from module module " + moduleName, ex);;
            return false;

        }

        return true;

    }

    public Dataset applyWorkflow(ProgressHandler handler, Dataset dataset, Workflow workflow) {
        setRunning(true);
        String source = dataset.getSource();

        for (WorkflowStep step : workflow.getStepList()) {

            handler.increment(1.0);
            handler.setStatus(dataset.getName());
            Module module = moduleService.createModule(step.getModule().getInfo());
            String moduleName = module.getInfo().getName();

            // injecting main input
            injectInput(dataset, module);

            // injecting parameters
            step.getParameters().forEach((key, value) -> {
                module.setInput(key, value);
                module.setResolved(key, true);
                module.resolveInput(key);
            });

            logger.info("Running module");
            Future<Module> run;

            // initializing the module
            try {
                getContext().inject(module);
                module.initialize();
            } catch (Exception e) {
                logger.info("Context already injected.");
            }
            List<PreprocessorPlugin> preProcessors = getPreProcessors(
                    ActiveDisplayPreprocessor.class,
                    ActiveDatasetPreprocessor.class,
                    ActiveDataViewPreprocessor.class,
                    ActiveDatasetViewPreprocessor.class,
                    ActiveImageDisplayPreprocessor.class
            );
            // running
            run = moduleService.run(module,
                    preProcessors,
                    getPostprocessors(), step.getParameters());

            logger.info(String.format("[%s] module started", moduleName));

            // waiting and extracting
            try {
                run.get();
                logger.info(String.format("[%s] module finished", moduleName));
                Dataset result = extractOutput(run.get());
                if (result != null) {
                    dataset = result;
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error when extracting ouput from module module " + moduleName, ex);;
                setRunning(false);
                return dataset;

            } finally {
                setRunning(false);
            }

        }
        dataset.setSource(source);
        return dataset;

    }

    private boolean injectInput(Dataset dataset, Module module) {
        ModuleItem item;
        logger.info("Injecting inputs into module : " + module.getDelegateObject().getClass().getSimpleName());
        item = moduleService.getSingleInput(module, Dataset.class);

        if (item != null) {
            logger.info("Dataset input field found : " + item.getName());

            if (dataset == null) {
                logger.warning("The Dataset for was null for ");
                return false;
            } else {
                module.setInput(item.getName(), dataset);
                logger.info("Injection done for " + item.getName() + " with " + dataset.toString());
                return true;
            }
        } else {
            item = moduleService.getSingleInput(module, ImageDisplay.class);

            if (item != null) {
                module.setInput(item.getName(), new SilentImageDisplay(getContext(), dataset));
                logger.info("Injection done using an ImageDisplay " + item.getName() + " with " + dataset.toString());
                return true;
            } else {
                return false;
            }
        }

    }

    private Dataset extractOutput(Module module) {

        Dataset output = (Dataset) module
                .getOutputs()
                .values()
                .stream()
                .filter(object -> object != null && Dataset.class.isAssignableFrom(object.getClass()))
                .findFirst()
                .orElse(null);

        if (output == null) {
            output = module
                    .getOutputs()
                    .values()
                    .stream()
                    .filter(object -> object != null && ImageDisplay.class.isAssignableFrom(object.getClass()))
                    .map(o -> imageDisplayService.getActiveDataset((ImageDisplay) o))
                    .findFirst()
                    .orElse(null);
        }

        return output;

    }

    // inject the dataset or display depending on the requirements of the module
    private boolean injectInput(BatchSingleInput input, Module module) {

        ModuleItem item;
        logger.info("Injecting inputs into module : " + module.getDelegateObject().getClass().getSimpleName());
        item = moduleService.getSingleInput(module, Dataset.class);

        if (item != null) {
            logger.info("Dataset input field found : " + item.getName());
            Dataset dataset = input.getDataset();

            if (dataset == null) {
                logger.info("The Dataset for was null for " + input.getName());
            } else {
                module.setInput(item.getName(), dataset);
                logger.info("Injection done for " + item.getName() + " with " + dataset.toString());
                return true;
            }
        }

        // testing if it takes a Display as input
        item = moduleService.getSingleInput(module, ImageDisplay.class);
        if (item != null) {
            logger.info("ImageDisplay input field found : " + item.getName());
            // if yes, injecting the display
            module.setInput(item.getName(), input.getDisplay());
            return true;
        }
        item = moduleService.getSingleInput(module, DatasetView.class);
        if (item != null) {
            logger.info("DatasetView field found : " + item.getName());
            // if yes, injecting the display
            DatasetView datasetView = input.getDatasetView();

            module.setInput(item.getName(), datasetView);
            return true;
        } else {
            logger.info("Error when injecting input !");
            return false;
        }

    }

    // extract the outpu from an executed module
    public void extractOutput(BatchSingleInput input, Module module) {
        Map<String, Object> outputs = module.getOutputs();
        outputs.forEach((s, o) -> {
            logger.info(String.format("Trying to find output from %s = %s", s, o));
            if (Dataset.class.isAssignableFrom(o.getClass())) {
                logger.info("Extracting Dataset !");
                input.setDataset((Dataset) module.getOutput(s));
            } else if (ImageDisplay.class.isAssignableFrom(o.getClass())) {
                logger.info("Extracting ImageDisplay !");
                input.setDisplay((ImageDisplay) module.getOutput(s));
            } else if (o instanceof DatasetView) {
                logger.info("Extracting DatasetView !");
                input.setDatasetView((DatasetView) module.getOutput(s));
            }
        });

    }

    public Module createModule(Class<? extends Command> clazz) {

        CommandInfo infos = commandService.getCommand(clazz);

        Module module;

        module = moduleService.createModule(infos);
        try {
            module.initialize();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error when initializing module...", ex);
        }

        return module;
    }

    public void preProcessExceptFor(Module module, Class<?>... blacklist) {

        pluginService
                .createInstancesOfType(PreprocessorPlugin.class)
                .stream()
                .sequential()
                .filter(pp -> ArrayUtils.contains(blacklist, pp.getClass()) == false)
                .forEach(pp -> {
                    pp.process(module);
                });

    }

    public void preProcessExceptFor(Module module, List<Class<?>> blacklist) {
        pluginService
                .createInstancesOfType(PreprocessorPlugin.class)
                .stream()
                .sequential()
                .filter(plugin -> !blacklist.contains(plugin.getClass()))
                .forEach(plugin -> plugin.process(module));
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    Class<? extends PreprocessorPlugin>[] preProcessorBlackList;

    private <T> T injectPlugin(T p) {
        try {
            getContext().inject(p);
        } catch (Exception e) {
            ImageJFX.getLogger().log(Level.SEVERE, null, e);
        } finally {
            return p;
        }
    }

    public List<PreprocessorPlugin> getPreProcessors() {
        return pluginService
                .createInstancesOfType(PreprocessorPlugin.class)
                .stream()
                .sequential()
                .filter(p -> !processorBlackList.contains(p.getClass()))
                .sequential()
                .map(p -> {
                    return p;
                })
                //.map(this::injectPlugin)
                .collect(Collectors.toList());
    }

    public List<PreprocessorPlugin> getPreProcessors(Class<?>... blacklist) {
        return pluginService
                .createInstancesOfType(PreprocessorPlugin.class)
                .stream()
                .sequential()
                .filter(p -> Stream.of(blacklist).filter(cl -> cl.equals(p.getClass())).count() == 0)
                .filter(p -> !processorBlackList.contains(p.getClass()))
                .sequential()
                .map(p -> {
                    return p;
                })
                //.map(this::injectPlugin)
                .collect(Collectors.toList());
    }

    public List<PostprocessorPlugin> getPostprocessors() {
        return pluginService
                .createInstancesOfType(PostprocessorPlugin.class)
                .stream()
                .sequential()
                .filter(p -> !processorBlackList.contains(p.getClass()))
                //.map(this::injectPlugin)
                .collect(Collectors.toList());
    }

    public BatchBuilder builder() {
        return new BatchBuilder(getContext());
    }

}

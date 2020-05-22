package atlas.plugin.replacetestfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.util.concurrent.NotNull;
import com.google.common.io.Files;
import com.google.gson.Gson;

public class BuildFailedTestListTask implements TaskType {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BuildFailedTestListTask.class);
    
    private static final Gson gson = new Gson();
    
    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext)
            throws TaskException {

        String fileNameWithDefaultTestClassesList = taskContext
                .getConfigurationMap().get(
                        BuildFailedTestListTaskConfigurator.testNameFileKey);

        String jobContent = taskContext.getRuntimeTaskContext().get(taskContext.getBuildContext().getPlanName());
        
        if (jobContent == null) {

            LOGGER.info("Replace plugin: readFileAndSaveJob_run - new job ");
            
            readFileAndSaveContent(taskContext, fileNameWithDefaultTestClassesList);

            return TaskResultBuilder.newBuilder(taskContext).success().build();
        }

        Job currentJob = gson.fromJson(jobContent, Job.class);
        
        if (currentJob.getResults().size() == 0) {
            return TaskResultBuilder.newBuilder(taskContext).success().build();
        }

        for (int i = 0; i < currentJob.getResults().size(); i++) {

            if (i == currentJob.getResults().size() - 1) {
                writeToFile(
                        taskContext.getWorkingDirectory().getAbsolutePath(),
                        "TestClasses.txt", currentJob.getResults().get(i));
                break;
            }

            if (i == 0) {
                writeToFile(
                        taskContext.getWorkingDirectory().getAbsolutePath(),
                        "TestClasses_run.txt", currentJob.getResults().get(i));
                continue;
            }

            writeToFile(taskContext.getWorkingDirectory().getAbsolutePath(),
                    "TestClasses_rerun_" + i + ".txt", currentJob.getResults()
                            .get(i));

        }


        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private void readFileAndSaveContent(TaskContext taskContext,
            String fileNameWithDefaultTestClassesList) {

        List<String> currentTestClasses = getTestClassesList(new File(
                taskContext.getWorkingDirectory().getAbsolutePath() + "/"
                        + fileNameWithDefaultTestClassesList));
        
        if (currentTestClasses == null || currentTestClasses.isEmpty()){
        	 LOGGER.info("Replace plugin: readFileAndSaveJob| getPlanName: "
                     + taskContext.getBuildContext().getPlanName()
                     + " currentTestClasses is null or empty ");
        	return;
        }

        String classes = String.join(HandlerProcessorServer.DELIM,
                currentTestClasses);

        taskContext.getRuntimeTaskContext().put(
                taskContext.getBuildContext().getPlanName(), classes);

        LOGGER.info("Replace plugin: readFileAndSaveJob| getPlanName: "
                + taskContext.getBuildContext().getPlanName()
                + " currentTestClasses : " + currentTestClasses.toString());
    }

    public Optional<String> getExtensionByString(String filename) {
        return Optional.ofNullable(filename).filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    private void writeToFile(String root, String fileName, Set<String> list) {

        String content = list.stream().collect(
                Collectors.joining(System.lineSeparator()));

        LOGGER.info("Replace plugin: content " + content);

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(root + "/" + fileName));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("Replace plugin: while writting to file has error "
                    + e.getMessage());
        }
    }

    private List<String> getTestClassesList(File testClassesFile) {
        if (!testClassesFile.exists()) {
            LOGGER.info("Replace plugin: " + testClassesFile.getName()
                    + " not exist");
        }
        List<String> classes = null;
        try {
            classes = Files
                    .readLines(testClassesFile, Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.error("Replace plugin: appeared error while reading the file - "
                    + e.getMessage());
        }

        return classes;
    }
}
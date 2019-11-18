package atlas.plugin.replacetestfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.util.concurrent.NotNull;
import com.google.common.io.Files;

public class BuildFailedTestListTask implements TaskType {

    public static final String PATTERN = "\\(.*?\\)";
    private static final Logger LOGGER = LoggerFactory
            .getLogger(BuildFailedTestListTask.class);

    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext)
            throws TaskException {

        Job job = (Job) taskContext.getRuntimeTaskData().get(
                taskContext.getBuildContext().getPlanName());

        if (job == null || job.getResults() == null) {
            LOGGER.info("Replace plugin: job or results are null");
            return TaskResultBuilder.newBuilder(taskContext).success().build();
        }

        if (taskContext.getBuildContext().getBuildNumber() == job
                .getBuildNumber() && job.getResults().size() > 0) {

            String fileName = taskContext.getConfigurationMap().get(
                    BuildFailedTestListTaskConfigurator.testNameFileKey);

            LOGGER.info("Replace plugin: taskContext.getRootDirectory().getAbsolutePath() = "
                    + taskContext.getRootDirectory().getAbsolutePath()
                    + " | taskContext.getRootDirectory().getPath()"
                    + taskContext.getRootDirectory().getPath()
                    + " | taskContext.getWorkingDirectory().getAbsolutePath()"
                    + taskContext.getWorkingDirectory().getAbsolutePath());

            String newCurrentFileName = getFileNameWithIndex(fileName, 0);

            if (newCurrentFileName == null) {
                LOGGER.info("Replace plugin: newCurrentFileName is null "
                        + fileName);
                return TaskResultBuilder.newBuilder(taskContext).success()
                        .build();
            }

            copyFile(taskContext.getWorkingDirectory().getAbsolutePath()
                    + "/" + fileName, taskContext.getWorkingDirectory()
                    .getAbsolutePath() + "/" + newCurrentFileName);

            createFilesByHistory(taskContext.getWorkingDirectory()
                    .getAbsolutePath(), fileName, job.getResults());
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private void createFilesByHistory(String root, String fileName,
            List<Collection<TestResults>> failedTest) {

        List<String> listOfTestClasses = new ArrayList<>();
        try {
            failedTest
                    .stream()
                    .forEach(
                            collection -> {
                                StringBuilder classes = new StringBuilder();
                                collection
                                        .stream()
                                        .forEach(
                                                testResult -> {
                                                    String className = getClassName(testResult
                                                            .getActualMethodName());

                                                    if (className == null) {
                                                        LOGGER.info("Replace plugin: className is null = "
                                                                + testResult
                                                                        .getActualMethodName());
                                                        throw new NullPointerException();
                                                    }

                                                    if (classes
                                                            .indexOf(className) == -1) {
                                                        classes.append(className);
                                                        classes.append(System.getProperty("line.separator"));
                                                        LOGGER.info("Replace plugin: className = "
                                                                + className
                                                                + ". Actual name = "
                                                                + testResult
                                                                        .getActualMethodName());
                                                    }
                                                });
                                listOfTestClasses.add(classes.toString());
                            });

            for (int i = 0; i < listOfTestClasses.size(); i++) {
                String newFileName = null;
                if (i < listOfTestClasses.size() - 1) {
                    newFileName = getFileNameWithIndex(fileName, i + 1);
                } else {
                    newFileName = fileName;
                }

                LOGGER.info("Replace plugin: path = " + root + "/"
                        + newFileName);
                writeToFile(root, newFileName, listOfTestClasses.get(i));
            }

        } catch (NullPointerException e) {
            return;
        }

        LOGGER.info("Replace plugin: replaceFile -> failedTest = "
                + failedTest.toString());

    }

    private String getFileNameWithIndex(String fileName, int index) {
        Optional<String> ext = getExtensionByString(fileName);
        if (ext.isPresent()) {
            return fileName.replace("." + ext.get(), index + "." + ext.get());
        }
        return null;
    }

    public Optional<String> getExtensionByString(String filename) {
        return Optional.ofNullable(filename).filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    private void copyFile(String fullPathFrom, String fullPathTo) {
        try {
            LOGGER.info("Replace plugin: coping file. FullPathFrom: "
                    + fullPathFrom + " | FullPathTo: " + fullPathTo);
            Files.copy(new File(fullPathFrom), new File(fullPathTo));
        } catch (IOException e) {
            LOGGER.info("Replace plugin: coping file finished with error + "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    private void writeToFile(String root, String fileName, String content) {

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(root + "/" + fileName));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getClassName(String str) {
        return getStringByTemplate(PATTERN, str);
    }

    @Nullable
    private String getStringByTemplate(String pattern, String str) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);
        if (m.find()) {
            return m.group().subSequence(1, m.group().length() - 1).toString();
        } else {
            return null;
        }
    }
}

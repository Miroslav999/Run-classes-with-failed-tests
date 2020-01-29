package atlas.plugin.replacetestfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.build.CustomBuildProcessorServer;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.runtime.RuntimeTaskDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;

public class HandlerProcessorServer implements CustomBuildProcessorServer {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(HandlerProcessorServer.class);
    public static final String PATTERN = "\\(.*?\\)";
    public static final String TASK_KEY = "prom.atlas.plugins.rerun-failed-tests:replace-testname-file";
    public static final String DELIM = ",";
    private BuildContext buildContext;
    private Storage storage;

    public HandlerProcessorServer(Storage storage) {
        this.storage = storage;
    }

    @Override
    public BuildContext call() throws InterruptedException, Exception {

        TaskResult task = getTaskExporter(buildContext);

        if (task == null) {
            return buildContext;
        }

        String currentPlanName = buildContext.getPlanName();

        LOGGER.info("Replace plugin: currentPlanName: " + currentPlanName );
        
        if (buildContext.getCurrentResult().getBuildState()
                .equals(BuildState.SUCCESS)) {

            storage.getPlans().remove(currentPlanName);

            LOGGER.info("Replace plugin: currentPlanName was removed "
                    + currentPlanName);
            return buildContext;
        }

        Job job = getJob();
        
        List<String> lastRunningClasses = getLastRunningClasses(job);
        
        List<String> newListRunningClasses = new ArrayList<>(lastRunningClasses);
        
        List<String> classesWithFailedTests = getClasses(buildContext
                .getBuildResult().getFailedTestResults());

        List<String> classesWithSuccessTests = getClasses(buildContext
                .getBuildResult().getSuccessfulTestResults());

        newListRunningClasses.removeAll(classesWithSuccessTests);

        newListRunningClasses.removeAll(classesWithFailedTests);

        newListRunningClasses.addAll(classesWithFailedTests);

        job.addResults(newListRunningClasses);
        
        job.increaseNumberOfRetries();

        LOGGER.info("Replace plugin: classesWithFailedTests : "
                + classesWithFailedTests.toString() + "\n"
                + "classesWithSuccessTests : + "
                + classesWithSuccessTests.toString() + "\n"
                + "lastRunningClasses : " + lastRunningClasses.toString() + "\n"
                + "newListRunningClasses : " + newListRunningClasses.toString());

        return buildContext;
    }
    
    private List<String> getLastRunningClasses(Job job){
        
        if (job.getBuildNumber() != buildContext.getBuildNumber()){
            RuntimeTaskDefinition taskDefinition = getTaskDefinition(buildContext);

            if (taskDefinition == null) {
                LOGGER.info("Replace plugin: RuntimeTaskDefinition is empty ");
                return null;
            }
            String classes = taskDefinition.getRuntimeContext().get(buildContext.getPlanName());
     
            LOGGER.info("Replace plugin: content on server : " + classes);
            
            job.getResults().clear();
            
            job.setBuildNumber(buildContext.getBuildNumber());
            
            job.resetNumberOfRetries();
            
            List<String> lastRunningClasses = new ArrayList<>();
            
            lastRunningClasses.addAll(Arrays.asList(classes.split(DELIM)));
            
            job.getResults().add(lastRunningClasses);
            
            return job.getResults().get(0);
        }
        
        return job.getResults().get(job.getNumberOfRetries());
    }

    private Job getJob(){
        Job job = storage.getPlans().get(buildContext.getPlanName());
        if (job == null){
            job = new Job();
            storage.getPlans().put(buildContext.getPlanName(), job);
        }
        return job;
    }
    
    @Override
    public void init(BuildContext arg0) {
        this.buildContext = arg0;
    }

    @Nullable
    private TaskResult getTaskExporter(BuildContext buildContext) {
        return buildContext
                .getBuildResult()
                .getTaskResults()
                .stream()
                .filter(task -> task.getTaskIdentifier().getPluginKey()
                        .equals(TASK_KEY)).findFirst().orElse(null);
    }

    @Nullable
    private RuntimeTaskDefinition getTaskDefinition(BuildContext buildContext) {
        return buildContext
                .getRuntimeTaskDefinitions()
                .stream()
                .filter(taskDefinition -> taskDefinition.getPluginKey()
                        .equals(TASK_KEY)).findFirst().orElse(null);
    }

    private List<String> getClasses(Collection<TestResults> tests) {
        List<String> classes = new ArrayList<>();
        tests.stream().forEach(result -> {
            //String className = getClassName(result.getActualMethodName());
            String className = result.getActualMethodName();
            if (classes.indexOf(className) == -1) {
                classes.add(className);
            }
        });
        return classes;
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

package atlas.plugin.replacetestfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    private BuildContext buildContext;
    private Storage storage;

    public HandlerProcessorServer(Storage storage) {
        this.storage = storage;
    }

    @Override
    public BuildContext call() throws InterruptedException, Exception {

        Optional<TaskResult> task = getTaskExporter(buildContext);

        if (!task.isPresent()) {
            return buildContext;
        }

        String currentPlanName = buildContext.getPlanName();

        if (buildContext.getCurrentResult().getBuildState()
                .equals(BuildState.SUCCESS)) {
            
            storage.getPlans().remove(currentPlanName);
            
            LOGGER.info("Replace plugin: currentPlanName was removed "
                    + currentPlanName);
            return buildContext;
        }

        Optional<RuntimeTaskDefinition> taskDefinition = getTaskDefinition(buildContext);
        
        if (!taskDefinition.isPresent()){
            LOGGER.info("Replace plugin: RuntimeTaskDefinition is empty ");
            return buildContext;
        }
           
        Job job = (Job) taskDefinition.get().getRuntimeData().get(currentPlanName);

        LOGGER.info("Replace plugin: got job on server : " + job.getBuildNumber() + " / " + job.getResults().toString());
        
        List<String> classesWithFailedTests = getClasses(buildContext.getBuildResult().getFailedTestResults());
        
        List<String> classesWithSuccessTests = getClasses(buildContext.getBuildResult().getSuccessfulTestResults());
       
        List<String> lastRunningClasses = new ArrayList <>(job.getResults().get(job.getNumberOfRetries()));

        lastRunningClasses.removeAll(classesWithSuccessTests);
        
        lastRunningClasses.removeAll(classesWithFailedTests);
        
        lastRunningClasses.addAll(classesWithFailedTests);
        
        job.addResults(lastRunningClasses);
        
        storage.getPlans().remove(currentPlanName);
        
        storage.getPlans().put(currentPlanName, job);
        
        LOGGER.info("Replace plugin: classesWithFailedTests : " + classesWithFailedTests.toString() + "\n " + "classesWithSuccessTests : + "
                + classesWithSuccessTests.toString() + "\n " + 
                "lastRunningClasses : " + lastRunningClasses.toString());

        
        return buildContext;
    }

    @Override
    public void init(BuildContext arg0) {
        this.buildContext = arg0;
    }

    @Nullable
    private Optional<TaskResult> getTaskExporter(BuildContext buildContext) {
        return buildContext
                .getBuildResult()
                .getTaskResults()
                .stream()
                .filter(task -> task.getTaskIdentifier().getPluginKey()
                        .equals(TASK_KEY)).findFirst();
    }
    
    @Nullable
    private Optional<RuntimeTaskDefinition> getTaskDefinition(BuildContext buildContext){
        return buildContext
                .getRuntimeTaskDefinitions()
                .stream()
                .filter(taskDefinition -> taskDefinition.getPluginKey().equals(
                        HandlerProcessorServer.TASK_KEY)).findFirst();
    }
    
    private List<String> getClasses(Collection<TestResults> tests){
        List<String> classes = new ArrayList<>();
        tests.stream().forEach(result -> {
            String className = getClassName(result.getActualMethodName());
            if (classes.indexOf(className) == -1){
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

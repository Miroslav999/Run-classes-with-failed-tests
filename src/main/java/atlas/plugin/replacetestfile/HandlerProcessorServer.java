package atlas.plugin.replacetestfile;

import java.util.Optional;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.build.CustomBuildProcessorServer;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.v2.build.BuildContext;

public class HandlerProcessorServer implements CustomBuildProcessorServer {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(HandlerProcessorServer.class);
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

        if (buildContext.getCurrentResult().getBuildState().equals(BuildState.SUCCESS)) {
            storage.getPlans().remove(currentPlanName);
            LOGGER.info("Replace plugin: currentPlanName was removed "
                    + currentPlanName);
            return buildContext;
        }

        Job job = storage.getPlans().get(currentPlanName);

        if (job == null) {
            createJob(currentPlanName);
        } else {
            saveJob(job);
        }

        return buildContext;
    }

    private void createJob(String currentPlanName) {
        Job job = new Job(buildContext.getBuildNumber());
        job.addResults(buildContext.getBuildResult().getFailedTestResults());
        storage.getPlans().put(currentPlanName, job);
        LOGGER.info("Replace plugin: job was created. BuildNumber : "
                + buildContext.getBuildNumber() + " size failed : "
                + buildContext.getBuildResult().getFailedTestResults().size());
    }

    private void saveJob(Job job) {
        LOGGER.info("Replace plugin: job.getBuildNumber()"
                + job.getBuildNumber() + " size failed : "
                + buildContext.getBuildResult().getFailedTestResults().size());
        if (job.getBuildNumber() != buildContext.getBuildNumber()) {
            job.getResults().clear();
            job.setBuildNumber(buildContext.getBuildNumber());
        } else {
            job.addResults(buildContext.getBuildResult().getFailedTestResults());
        }
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

}

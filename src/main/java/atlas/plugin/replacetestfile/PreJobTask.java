package atlas.plugin.replacetestfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.chains.StageExecution;
import com.atlassian.bamboo.chains.plugins.PreJobAction;
import com.atlassian.bamboo.v2.build.BuildContext;

public class PreJobTask implements PreJobAction{
    
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PreJobTask.class);
    
    private Storage storage;
    
    public PreJobTask(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void execute(StageExecution stageExecution, BuildContext buildContext) {
        buildContext
                .getRuntimeTaskDefinitions()
                .stream()
                .filter(task ->  task.getPluginKey().equals(HandlerProcessorServer.TASK_KEY))
                .findFirst()
                .ifPresent(task -> {
                    Job job = storage.getPlans().get(buildContext.getPlanName());
                    task.getRuntimeData().put(buildContext.getPlanName(), job);
                    LOGGER.info("Replace plugin: PreJobTask  -> " +  buildContext.getPlanName() + " job: " + job.getResults().size());
                });
    }

}

package atlas.plugin.replacetestfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.chains.StageExecution;
import com.atlassian.bamboo.chains.plugins.PreJobAction;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.google.gson.Gson;

public class PreJobTask implements PreJobAction{
    
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PreJobTask.class);
    
    private static final Gson gson = new Gson();
    
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
                    if (job != null && job.getBuildNumber() == buildContext.getBuildNumber()){
                        String content = gson.toJson(job);
                        task.getRuntimeContext().put(buildContext.getPlanName(),content);
                        LOGGER.info("Replace plugin: PreJobTask  -> " +  buildContext.getPlanName() + " job: " + content);
                    }
                });
    }

}

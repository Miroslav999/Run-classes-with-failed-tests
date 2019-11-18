package atlas.plugin.replacetestfile;

import java.util.Map;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.util.concurrent.NotNull;
import com.atlassian.util.concurrent.Nullable;

public class BuildFailedTestListTaskConfigurator extends AbstractTaskConfigurator {
	
	private String testNameFileValue;
	public static final String testNameFileKey = "testnamefile";
	private String errorMsg = "The field can't be empty";

	public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
	{
	    final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

	    config.put(testNameFileKey, params.getString(testNameFileKey));
	  
	    return config;
	}
	
	@Override
	public void populateContextForCreate(@NotNull final Map<String, Object> context)
	{
	    super.populateContextForCreate(context);
	    if (testNameFileValue != null){
	    	context.put(testNameFileKey, testNameFileValue);
	    }
	}
	
	@Override
	public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
	{
	    super.populateContextForEdit(context, taskDefinition);
	   
	    testNameFileValue = taskDefinition.getConfiguration().get(testNameFileKey);
	    context.put(testNameFileKey, testNameFileValue);
	}

	@Override
	public void validate(ActionParametersMap params,
			ErrorCollection errorCollection) {
		super.validate(params, errorCollection);
		
		final String testTypeValue = params.getString(testNameFileKey);
	    if (testTypeValue == null || testTypeValue.isEmpty()){
	        errorCollection.addError("testType", errorMsg);
	    }

	}
	
	
}

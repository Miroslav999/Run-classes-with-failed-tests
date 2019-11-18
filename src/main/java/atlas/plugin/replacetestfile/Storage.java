package atlas.plugin.replacetestfile;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class Storage {
	private Map<String, Job> plans = new HashMap<>();

	public Map<String, Job> getPlans() {
		return plans;
	}
}

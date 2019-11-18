package atlas.plugin.replacetestfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.serialization.WhitelistedSerializable;

public class Job implements WhitelistedSerializable{

    private static final long serialVersionUID = 4477444309360773529L;
    private int buildNumber;
	private List<Collection<TestResults>> results = new ArrayList<>();
	
	public Job(){}
	
	public Job(int buildNumber) {
		this.buildNumber = buildNumber;
	}

	public List<Collection<TestResults>> getResults() {
		return results;
	}
	public void addResults(Collection<TestResults> results) {
		this.results.add(results);
	}
	public int getBuildNumber() {
		return buildNumber;
	}
	
	public void setBuildNumber(int buildNumber) {
		this.buildNumber = buildNumber;
	}
}

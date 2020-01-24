package atlas.plugin.replacetestfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.atlassian.bamboo.serialization.WhitelistedSerializable;

public class Job implements WhitelistedSerializable{

    private static final long serialVersionUID = 4477444309360773529L;
    private int buildNumber = 0;
	private List<Set<String>> results = new ArrayList<>();
	private int numberOfRetries = 0;
	
	public Job(){}
	
	public List<Set<String>> getResults() {
		return results;
	}
	public void addResults(Set<String> results) {
		this.results.add(results);
	}
	public int getBuildNumber() {
		return buildNumber;
	}
	
	public void setBuildNumber(int buildNumber) {
		this.buildNumber = buildNumber;
	}

    public void increaseNumberOfRetries() {
        this.numberOfRetries++;
    }

    public int getNumberOfRetries() {
        return numberOfRetries;
    }
    
    public void resetNumberOfRetries(){
        numberOfRetries = 0;
    }
	
}

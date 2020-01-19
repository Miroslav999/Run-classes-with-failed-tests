package atlas.plugin.replacetestfile;

import java.util.ArrayList;
import java.util.List;
import com.atlassian.bamboo.serialization.WhitelistedSerializable;

public class Job implements WhitelistedSerializable{

    private static final long serialVersionUID = 4477444309360773529L;
    private int buildNumber = 0;
	private List<List<String>> results = new ArrayList<>();
	private int numberOfRetries = 0;
	
	public Job(){}
	
	public List<List<String>> getResults() {
		return results;
	}
	public void addResults(List<String> results) {
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

/*   Copyright (C) 2013-2014 Computer Sciences Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package ezbake.azkaban.manager;

import ezbake.azkaban.client.http.HttpManager;
import ezbake.azkaban.manager.result.AuthenticationResult;
import ezbake.azkaban.manager.result.ExecutionResult;
import ezbake.azkaban.manager.result.RunningExecutionsResult;
import ezbake.azkaban.submitter.util.JsonUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for executing a flow on Azkaban
 */
public class ExecutionManager {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionManager.class);
	
	private static class OptionsBean {
		@Option(name="-u", aliases="--username", usage="username", required=true)
	    String username;

	    @Option(name="-p", aliases="--password", usage="password", required=true)
	    String password;

	    @Option(name="-e", aliases="--executionUri", usage="azkaban endpoint", required=true)
	    String endPoint;

        @Option(name="-n", aliases="--name", usage="project name", required=true)
        String name;

        @Option(name="-f", aliases="--flow", usage="flow to execute", required=true)
        String flow;
	}

    private String sessionId;
    private URI executionUri; // The URL to hit

    /**
     * Class for executing a flow in Azkaban
     *
     * @param sessionId The session ID of an already connected session
     * @param azkabanUri The Azkaban URL
     */
    public ExecutionManager(String sessionId, URI azkabanUri){
        try {
            this.executionUri = new URIBuilder(azkabanUri).setPath("/executor").build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.sessionId = sessionId;
    }

    /**
     * Class for executing a flow in Azkaban
     *
     * @param azkabanUri The Azkaban URL
     * @param username The username to use
     * @param password The password for the username
     */
    public ExecutionManager(URI azkabanUri, String username, String password) {
        try {
            this.executionUri = new URIBuilder(azkabanUri).setPath("/executor").build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final AuthenticationManager azkabanAuthenticator = new AuthenticationManager(azkabanUri, username, password);
        final AuthenticationResult result = azkabanAuthenticator.login();
        if(result.hasError()){
            throw new IllegalStateException(result.getError());
        }
        this.sessionId = result.getSessionId();
    }


    /**
     * Returns all of the current executions of the flow
     *
     * @param projectName The project to fetch from
     * @param flowId The id of the flow to executeFlow the exections for
     * @return List of flow ID's that are currently executing
     */
    public RunningExecutionsResult getRunningExecutions(String projectName, String flowId) throws Exception {
        final URI uri = new URIBuilder(executionUri)
                .setParameter("session.id", sessionId)
                .setParameter("ajax", "getRunning")
                .setParameter("project", projectName)
                .setParameter("flow", flowId)
                .build();

        final HttpGet get = new HttpGet(uri);
        final String json = HttpManager.get(get);
        logger.info("Running executions result: \n{}", json);

        return (RunningExecutionsResult) JsonUtil.deserialize(json, RunningExecutionsResult.class);
    }

    /**
     * Cancels the executing flow
     *
     * @param executionId The execution ID of the flow to cancel
     * @return Empty String if no errors, or a message if there was a problem
     * @throws Exception
     */
    public String cancelFlow(String executionId) throws Exception {
        final URI uri = new URIBuilder(executionUri)
                .setParameter("session.id", sessionId)
                .setParameter("ajax", "cancelFlow")
                .setParameter("execid", executionId)
                .build();

        final HttpGet get = new HttpGet(uri);
        final String json = HttpManager.get(get);
        logger.info("Cancel Flow result: \n{}", json);
        return json.isEmpty() ? json : "Flow isn't running";
    }

    /**
     * Logs into Azkaban and returns the result with the session ID
     * @param projectName The project name containing the flow to execute
     * @param flow The flow to execute
     * @return {@link ezbake.azkaban.manager.result.AuthenticationResult} containing the session ID if successful
     */
	public ExecutionResult executeFlow(String projectName, String flow) {
		try {
			final List<NameValuePair> postPairs = new ArrayList<>();
			postPairs.add(new BasicNameValuePair("session.id", sessionId));
			postPairs.add(new BasicNameValuePair("ajax", "executeFlow"));
			postPairs.add(new BasicNameValuePair("project", projectName));
            postPairs.add(new BasicNameValuePair("flow", flow));
			
			final HttpPost post = new HttpPost(executionUri);
			post.setEntity(new UrlEncodedFormEntity(postPairs));

			final String json = HttpManager.post(post);

			return (ExecutionResult) JsonUtil.deserialize(json, ExecutionResult.class);
		} catch(Exception ex) {
			ex.printStackTrace();
			return new ExecutionResult(ex.getMessage());
		}
	}

    public static void main(String[] args) throws Exception {
        final OptionsBean optionsBean = new OptionsBean();
        final CmdLineParser parser = new CmdLineParser(optionsBean);
        
        try {
            parser.parseArgument(args);
            final ExecutionResult result = new ExecutionManager(new URI(optionsBean.endPoint),
            		optionsBean.username, optionsBean.password).executeFlow(optionsBean.name, optionsBean.flow);

            if(result.hasError()) {
                System.err.println(result.getError());
            } else {
                System.out.println(result.getMessage());
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }
}

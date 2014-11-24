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
import ezbake.azkaban.manager.result.ManagerResult;
import ezbake.azkaban.manager.result.ProjectFlowsResult;
import ezbake.azkaban.manager.result.RemoveScheduleResult;
import ezbake.azkaban.manager.result.RunningExecutionsResult;
import ezbake.azkaban.submitter.util.JsonUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to manage all aspects of projects in Azkaban
 */
public class ProjectManager {

    private static final Logger logger = LoggerFactory.getLogger(ProjectManager.class);

    private URI azkabanUri;
    private URI managerUri;
    private String sessionId;

    /**
     * Manages Azkaban projects
     *
     * @param azkabanUri The URL of the Azkaban server
     * @param username The username for the project
     * @param password The password for the project
     */
    public ProjectManager(URI azkabanUri, String username, String password){
        this.azkabanUri = azkabanUri;
        try {
            this.managerUri = new URIBuilder(azkabanUri).setPath("/manager").build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.sessionId = new AuthenticationManager(azkabanUri, username, password).login().getSessionId();
    }

    /**
     * Manages Azkaban projects
     *
     * @param sessionId The sessionId of the logged in Azkaban user
     * @param azkabanUri The URL of the Azkaban server
     */
    public ProjectManager(String sessionId, URI azkabanUri){
        this.azkabanUri = azkabanUri;
        try {
            this.managerUri = new URIBuilder(azkabanUri).setPath("/manager").build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.sessionId = sessionId;
    }

    /**
     * Creates a project in Azkaban for uploading .zip files to
     *
     * @param projectName The name of the project to create
     * @param projectDescription The description for the project
     * @return {@link ezbake.azkaban.manager.result.ManagerResult} with the status of the command
     */
    public ManagerResult createProject(String projectName, String projectDescription){
        final List<NameValuePair> postPairs = new ArrayList<>();
        postPairs.add(new BasicNameValuePair("session.id", this.sessionId));
        postPairs.add(new BasicNameValuePair("action", "create"));
        postPairs.add(new BasicNameValuePair("name", projectName));
        postPairs.add(new BasicNameValuePair("description", projectDescription));

        try {
            final HttpPost post = new HttpPost(managerUri);
            post.setEntity(new UrlEncodedFormEntity(postPairs));
            final String json = HttpManager.post(post);
            return (ManagerResult) JsonUtil.deserialize(json, ManagerResult.class);
        } catch (Exception e) {
            return new ManagerResult(e.getMessage());
        }
    }


    /**
     * Fetches the flows of a project
     *
     * @param projectName The project to fetch from
     * @return All of the flows for the project,  NULL if the project could not be found.
     * @throws Exception
     */
    public ProjectFlowsResult fetchProjectFlows(String projectName) throws Exception {
        final URI uri = new URIBuilder(managerUri)
                .setParameter("session.id", sessionId)
                .setParameter("ajax","fetchprojectflows")
                .setParameter("project", projectName)
                .build();

        final HttpGet get = new HttpGet(uri);
        final String json = HttpManager.get(get);
        logger.info("Fetch Projects result: \n{}", json);

        if(json == null || json.isEmpty()){
            return null;
        } else {
            return (ProjectFlowsResult) JsonUtil.deserialize(json, ProjectFlowsResult.class);
        }
    }

    /**
     * Attempts to delete the project.   Note that the Azkaban API doesn't return anything, so you have no idea if the
     * deletion was successful or not
     *
     * @param projectName The name of the project to delete
     */
    public void deleteProject(String projectName) throws Exception {
        final URI uri = new URIBuilder(managerUri)
                .setParameter("session.id", sessionId)
                .setParameter("delete", "true")
                .setParameter("project", projectName)
                .build();

        HttpManager.get(new HttpGet(uri));
    }

    /**
     * Removes the project from Azkaban
     *
     * @param projectName The project to remove
     * @return The ID number of the project that was removed
     */
    public String removeProject(String projectName) throws Exception {
        final ExecutionManager executionManager = new ExecutionManager(sessionId, azkabanUri);
        final ScheduleManager  scheduleManager  = new ScheduleManager (sessionId, azkabanUri);

        // Fetch flows for the project
        final ProjectFlowsResult flowsResult = fetchProjectFlows(projectName);
        final List<ProjectFlowsResult.FlowId> flows = flowsResult.getFlows();

        // Find any currently running flows
        for(ProjectFlowsResult.FlowId flowId : flows){
            // Un-schedule any upcoming flows
            final RemoveScheduleResult removeScheduleResult = scheduleManager.removeSchedule(flowsResult.getProjectId(),
                    flowId.getFlowId());
            if(removeScheduleResult.hasError()){
                logger.error("Could not un-schedule flow {}: {}", flowId.getFlowId(), removeScheduleResult.getMessage());
                throw new Exception("Could not un-schedule flow: " + removeScheduleResult.getMessage());
            }

            logger.info("Checking for executions for flow ID '{}'", flowId.getFlowId());
            RunningExecutionsResult runningExecutionsResult = executionManager.getRunningExecutions(projectName, flowId.getFlowId());
            if(runningExecutionsResult != null && runningExecutionsResult.getExecIds() != null){
                for(String executionId : runningExecutionsResult.getExecIds()){
                    // Cancel the execution
                    logger.info("Canceling execution ID {}", executionId);
                    String result = executionManager.cancelFlow(executionId);
                    if(!result.isEmpty()){
                        logger.warn("Tried to cancel execution {} but it was not running.", executionId);
                    }
                }
            }
        }

        // Now that there is nothing running or scheduled to run, attempt to delete the project
        logger.info("Attempting to delete project");
        deleteProject(projectName);

        // Verify project was deleted.
        final ProjectFlowsResult verify = fetchProjectFlows(projectName);
        if(verify != null && !verify.getFlows().isEmpty()){
            throw new Exception("There are still flows for the the project.  Delete unsuccessful.");
        }

        return flowsResult.getProjectId();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Attempting to delete project: " + args[0]);
        final URI azkabanUri = new URI("https://az01:8443");
        final ProjectManager manager = new ProjectManager(azkabanUri, args[1], args[2]);
        System.out.println("Removed project #" + manager.removeProject(args[0]));
    }
}

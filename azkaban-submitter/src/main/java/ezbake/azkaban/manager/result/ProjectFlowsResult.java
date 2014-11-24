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

package ezbake.azkaban.manager.result;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * Results from the ProjectManager
 */
public class ProjectFlowsResult {

    private String project;
    private String projectId;
    private List<FlowId> flows;

    public ProjectFlowsResult() { }

    @JsonProperty(value="project")
    public String getProject() {
        return project;
    }
    public void setProject(String project) {
        this.project = project;
    }

    @JsonProperty(value="projectId")
    public String getProjectId() {
        return projectId;
    }
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @JsonProperty(value="flows")
    public List<FlowId> getFlows() {
        return flows;
    }
    public void setFlows(List<FlowId> flows) {
        this.flows = flows;
    }

    public static class FlowId {
        @JsonProperty("flowId")
        private String flowId;

        public String getFlowId() {
            return flowId;
        }

        public void setFlowId(String flowId) {
            this.flowId = flowId;
        }
    }

}

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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ExecutionResult {

    private String message;
    private String project;
    private String flow;
    private String execid;

    private String error;

    public ExecutionResult() { }

    public ExecutionResult(String error) {
        this.error = error;
    }

    @JsonProperty(value="error")
    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }

    @JsonProperty(value="flow")
    public String getFlow() {
        return flow;
    }
    public void setFlow(String flow) {
        this.flow = flow;
    }

    @JsonProperty(value="execid")
    public String getExecId() {
        return execid;
    }
    public void setExecId(String execid) {
        this.execid = execid;
    }

    @JsonProperty(value="message")
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty(value="project")
    public String getProject() {
        return project;
    }
    public void setProject(String project) {
        this.project = project;
    }

    @JsonIgnore
    public boolean hasError() {
        return null != error && !"".equals(error);
    }
}

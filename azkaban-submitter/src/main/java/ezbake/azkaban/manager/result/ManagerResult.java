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

/**
 * Results from the ProjectManager
 */
public class ManagerResult {

    private String action;
    private String status;
    private String message;
    private String params;
    private String path;

    public ManagerResult() { }

    public ManagerResult(String error) {
        this.status = "error";
        this.message = error;
    }

    @JsonProperty(value="path")
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty(value="action")
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }

    @JsonProperty(value="params")
    public String getParams() {
        return params;
    }
    public void setParams(String params) {
        this.params = params;
    }

    @JsonProperty(value="status")
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty(value="message")
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonIgnore
    public boolean hasError() {
        return "error".compareTo(status) == 0;
    }
}

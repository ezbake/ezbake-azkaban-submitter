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
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

public class FetchScheduleResult {

    private String error;

    @JsonProperty(value="schedule")
    private Schedule schedule;

    public FetchScheduleResult(){}
    public FetchScheduleResult(String error){
        this.error = error;
    }

    @JsonProperty(value="error")
    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }

    @JsonIgnore
    public boolean hasError() {
        return null != error && !"".equals(error);
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    @JsonIgnoreProperties({"submitUser", "firstSchedTime", "nextExecTime", "period"})
    public class Schedule {
        @JsonProperty(value="scheduleId")
        private int scheduleId;

        // Ignored for now
        private String submitUser;
        private String firstSchedTime;
        private String nextExecTime;
        private String period;

        public int getScheduleId() {
            return scheduleId;
        }

        public void setScheduleId(int scheduleId) {
            this.scheduleId = scheduleId;
        }
    }
}

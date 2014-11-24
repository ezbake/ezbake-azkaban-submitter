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
import ezbake.azkaban.manager.result.RemoveScheduleResult;
import ezbake.azkaban.manager.result.SchedulerResult;
import ezbake.azkaban.submitter.util.JsonUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.thrift.TException;
import org.codehaus.jackson.JsonParseException;
import org.joda.time.LocalTime;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for scheduling a flow in Azkaban
 */
public class ScheduleManager {

    private static Logger logger = LoggerFactory.getLogger(ScheduleManager.class);

    private static class OptionsBean {
        @Option(name="-u", aliases="--username", usage="username", required=true)
        String username;

        @Option(name="-p", aliases="--password", usage="password", required=true)
        String password;

        @Option(name="-e", aliases="--schedulerUri", usage="Azkaban URL", required=true)
        String endPoint;

        @Option(name="-n", aliases="--name", usage="project name", required=true)
        String name;

        @Option(name="-f", aliases="--flow", usage="flow to execute", required=true)
        String flow;

        @Option(name="-i", aliases="--projId", usage="project ID", required=true)
        String projectId;

        @Option(name="-d", aliases="--date", usage="Date to run in MM/DD/YYYY format (default NOW)", required=false)
        String scheduleDate;

        @Option(name="-t", aliases="--time", usage="Time of day to run in 12,00,pm,utc format (default NOW)", required=false)
        String scheduleTime;

        @Option(name="-q", aliases="--period", usage="frequency to run in <int>[Mwdhms] format", required=false)
        String period;
    }


    private String sessionId;
    private URI schedulerUri;

    private String scheduleDate;
    private String scheduleTime;

    // Optional parameters
    private String period;

    /**
     * Class for scheduling a flow in Azkaban
     *
     * @param azkabanUri The Azkaban URL
     * @param username The username to use
     * @param password The password for the username
     */
    public ScheduleManager(URI azkabanUri, String username, String password) {
        try {
            this.schedulerUri = new URIBuilder(azkabanUri).setPath("/schedule").build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final AuthenticationManager azkabanAuthenticator = new AuthenticationManager(schedulerUri,username,password);
        final AuthenticationResult result = azkabanAuthenticator.login();
        if(result.hasError()){
            throw new IllegalStateException(result.getError());
        }
        this.sessionId = result.getSessionId();
    }

    /**
     * Class for scheduling a flow in Azkaban
     *
     * @param sessionId The Azkaban sessionId of a logged in user for the project
     * @param azkabanUri The Azkaban URL
     */
    public ScheduleManager(String sessionId, URI azkabanUri){
        this.sessionId = sessionId;
        try {
            this.schedulerUri = new URIBuilder(azkabanUri).setPath("/schedule").build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the date that the flow should be executed.  Should be in the form of MM/DD/YYYY
     *
     * @param date The date to execute the flow (MM/DD/YYYY)
     */
    public void setScheduleDate(String date){
        this.scheduleDate = date;
        // TODO - verify proper date format
    }

    /**
     * Sets the time that the flow should initially execute
     *
     * @param time The time of day to run in 12,00,pm,utc format
     */
    public void setScheduleTime(String time){
        this.scheduleTime = time;
        // TODO - verify proper time format
    }

    /**
     * Set the period of how often the flow should run.  Should be in the format of <int>[Mwdhms] where
     * M = months
     * w = weeks
     * d = days
     * h = hours
     * m = minutes
     * s = seconds
     *
     * @param period The period string in the format of: intValue[Mwdhms]
     * @throws java.lang.NumberFormatException if not in a valid format
     */
    public void setPeriod(String period){
        final char periodUnit = period.charAt(period.length() - 1);

        if(periodUnit != 'M' && periodUnit != 'w' && periodUnit != 'd' &&
                periodUnit != 'h' && periodUnit != 'm' && periodUnit != 's'){
            throw new IllegalArgumentException("'" + periodUnit +"' is an invalid period unit. Should be one of [Mwdhms]");
        }

        // Verify that the parts are indeed integers
        this.period = period;
    }

    /**
     * Attempts to remove the scheduled flow
     *
     * @param projectId The project NUMBER the flow is in
     * @param flowName The id of the flow to executeFlow the schedule for
     * @return Whether or not the operation was successful
     * @throws Exception - If the flow isn't scheduled
     */
    public RemoveScheduleResult removeSchedule(String projectId, String flowName) throws Exception {
        final List<NameValuePair> postPairs = new ArrayList<>();
        postPairs.add(new BasicNameValuePair("action", "removeSched"));
        postPairs.add(new BasicNameValuePair("session.id", sessionId));
        postPairs.add(new BasicNameValuePair("projectId", projectId));
        postPairs.add(new BasicNameValuePair("flowName", flowName));

        final HttpPost post = new HttpPost(schedulerUri);
        post.setEntity(new UrlEncodedFormEntity(postPairs));

        final String json = HttpManager.post(post);
        try {
            final RemoveScheduleResult result = (RemoveScheduleResult) JsonUtil.deserialize(json, RemoveScheduleResult.class);
            logger.info("Remove result: \n{}", json);
            return result;
        } catch (JsonParseException ex){
            logger.warn("Flow '{}' potentially not scheduled", flowName);
            final RemoveScheduleResult result = new RemoveScheduleResult();
            result.setResult("unknown");
            result.setMessage("Flow might not have been scheduled");
            return result;
        }
    }

    /**
     * Schedules a flow to run in Azkaban
     * @param projectName The project name containing the flow to execute
     * @param flow The flow to execute
     * @param projectId The ID of the project
     *
     * @return {@link ezbake.azkaban.manager.result.SchedulerResult} containing the results of the scheduling
     */
    public SchedulerResult scheduleFlow(String projectName, String flow, String projectId) {
        try {
            final List<NameValuePair> postPairs = new ArrayList<>();
            postPairs.add(new BasicNameValuePair("ajax", "scheduleFlow"));
            postPairs.add(new BasicNameValuePair("session.id", sessionId));
            postPairs.add(new BasicNameValuePair("projectName", projectName));
            postPairs.add(new BasicNameValuePair("projectId", projectId));
            postPairs.add(new BasicNameValuePair("flow", flow));
            postPairs.add(new BasicNameValuePair("scheduleDate", scheduleDate));

            // Create the time if one wasn't provided
            if(scheduleTime == null) {
                // Need to add 2 minutes because Azkaban won't actually schedule it if it's scheduled to run at the
                // current time or in the past.  If we only added one minute there's a race condition for the code
                // submitting before the clock rolls over to the next minute.
                final LocalTime now = LocalTime.now().plusMinutes(2);
                scheduleTime = now.getHourOfDay() + "," + now.getMinuteOfHour() + "," +
                        ((now.getHourOfDay() > 12) ? "pm" : "am") + "," + now.getChronology().getZone().toString();

                logger.warn("time option not provided.  Using: " + scheduleTime);
            }
            postPairs.add(new BasicNameValuePair("scheduleTime", scheduleTime));

            if(period != null){
                postPairs.add(new BasicNameValuePair("is_recurring", "on"));
                postPairs.add(new BasicNameValuePair("period", period));
            }

            logger.info("Attempting to schedule {}.{} on {} at {} reoccuring {}", projectName, flow, scheduleDate, scheduleTime,
                    (period != null ? period : "never"));

            final HttpPost post = new HttpPost(schedulerUri);
            post.setEntity(new UrlEncodedFormEntity(postPairs));

            final String json = HttpManager.post(post);
            return (SchedulerResult) JsonUtil.deserialize(json, SchedulerResult.class);
        } catch(Exception ex) {
            ex.printStackTrace();
            return new SchedulerResult(ex.getMessage());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, TException {
        final OptionsBean bean = new OptionsBean();
        final CmdLineParser parser = new CmdLineParser(bean);

        try {
            parser.parseArgument(args);

            String time;
            if(bean.scheduleTime == null){
                // Need to add 2 minutes because Azkaban won't actually schedule it if it's scheduled to run at the
                // current time or in the past.  If we only added one minute there's a race condition for the code
                // submitting before the clock rolls over to the next minute.
                final LocalTime now = LocalTime.now().plusMinutes(2);
                time = now.getHourOfDay() + "," + now.getMinuteOfHour() + "," +
                        ((now.getHourOfDay() > 12) ? "pm" : "am") + "," + now.getChronology().getZone().toString();

                System.out.println("time option not provided.  Using: " + time);
            } else {
                time = bean.scheduleTime;
            }

            final ScheduleManager azkabanScheduler = new ScheduleManager(new URI(bean.endPoint), bean.username, bean.password);
            azkabanScheduler.scheduleDate = bean.scheduleDate;
            azkabanScheduler.scheduleTime = time;

            if(bean.period != null){
                azkabanScheduler.setPeriod(bean.period);
            }

            final SchedulerResult result = azkabanScheduler.scheduleFlow(bean.name, bean.flow, bean.projectId);

            if(result.hasError()) {
                System.err.println(result.getError());
            } else {
                System.out.println("Scheduled flow <" + bean.flow + "> :" + result.getStatus() + " | " + result.getMessage());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }
}

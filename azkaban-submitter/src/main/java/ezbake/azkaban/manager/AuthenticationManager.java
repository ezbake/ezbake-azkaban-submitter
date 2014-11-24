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
import ezbake.azkaban.submitter.util.JsonUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for getting the SessionID for interacting with Azkaban
 */
public class AuthenticationManager {
	
	private static class OptionsBean {
		@Option(name="-u", aliases="--username", usage="username", required=true)
	    String username;

	    @Option(name="-p", aliases="--password", usage="password", required=true)
	    String password;

	    @Option(name="-e", aliases="--azkabanURI", usage="azkaban endpoint", required=true)
	    String endPoint;
	}

    private URI azkabanURI;
    private String username;
    private String password;

    /**
     * Class for getting the SessionID for interacting with Azkaban
     *
     * @param azkabanURI The URI to connect to to executeFlow the sessionID
     * @param username The username to use
     * @param password The password for the username
     */
    public AuthenticationManager(URI azkabanURI, String username, String password) {
        this.azkabanURI = azkabanURI;
        this.username = username;
        this.password = password;
    }

    /**
     * Logs into Azkaban and returns the result with the session ID
      * @return {@link ezbake.azkaban.manager.result.AuthenticationResult} containing the session ID if successful
     */
	public AuthenticationResult login() {
		try {
			final List<NameValuePair> pairs = new ArrayList<>();
			pairs.add(new BasicNameValuePair("action", "login"));
			pairs.add(new BasicNameValuePair("username", username));
			pairs.add(new BasicNameValuePair("password", password));

			HttpPost post = new HttpPost(azkabanURI);
			post.setEntity(new UrlEncodedFormEntity(pairs));
			
			String json = HttpManager.post(post);
			return (AuthenticationResult) JsonUtil.deserialize(json, AuthenticationResult.class);
		} catch(Exception ex) {
			ex.printStackTrace();
			return new AuthenticationResult(ex.getMessage());
		}
	}

    public static void main(String[] args) throws Exception {
        final OptionsBean optionsBean = new OptionsBean();
        final CmdLineParser parser = new CmdLineParser(optionsBean);
        
        try {
            parser.parseArgument(args);
            final AuthenticationResult result = new AuthenticationManager(new URI(optionsBean.endPoint),
            		optionsBean.username, optionsBean.password).login();

            if(result.hasError()) {
                System.err.println(result.getError());
            } else {
                System.out.println(result.getSessionId());
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }
}

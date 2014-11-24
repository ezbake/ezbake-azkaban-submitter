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
import ezbake.azkaban.manager.result.UploaderResult;
import ezbake.azkaban.submitter.util.JsonUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.net.URI;

public class UploadManager {
	
	private static class OptionsBean {
		@Option(name="-u", aliases="--username", usage="username", required=true)
	    String username;

	    @Option(name="-p", aliases="--password", usage="password", required=true)
	    String password;

	    @Option(name="-e", aliases="--endPoint", usage="azkaban endpoint", required=true)
	    String endPoint;
	    
	    @Option(name="-z", aliases="--zip", usage="zip file", required=true)
	    String zipPath;
	    
	    @Option(name="-n", aliases="--name", usage="project name", required=true)
	    String projectName;
	}

    /**
     * Uploads an Azkaban zip file to Azkaban
     *
     * @param sessionId The session ID of an authenticated Azkaban user
     * @param url The Azkaban URL
     * @param projectName The project to upload the zip to
     * @param zip The zip file containing the jobs, jar and properties
     */
    public UploadManager(String sessionId, String url, String projectName, File zip){
        this.sessionId = sessionId;
        this.endPoint = url + "/manager";
        this.projectName = projectName;
        this.zip = zip;
    }

	private String sessionId;
	private String endPoint;
	private File zip;
	private String projectName;

    /**
     * Uploads the zip file to Azkaban
     *
     * @return {@link ezbake.azkaban.manager.result.UploaderResult} status of the upload
     */
	public UploaderResult uploadZip() {
		try {
			HttpEntity entity = MultipartEntityBuilder
					.create()
					.addTextBody("session.id", sessionId)
					.addTextBody("ajax", "upload")
					.addBinaryBody("file", zip, ContentType.create("application/zip"), zip.getName())
					.addTextBody("project", projectName)
					.build();
			
			HttpPost post = new HttpPost(endPoint);
			post.setEntity(entity);
			
			String json = HttpManager.post(post);
			return (UploaderResult)JsonUtil.deserialize(json, UploaderResult.class);
		} catch(Exception ex) {
			return new UploaderResult(ex.getMessage());
		}
	}
	
	public static void main(String[] args) throws Exception {
        OptionsBean optionsBean = new OptionsBean();
        CmdLineParser parser = new CmdLineParser(optionsBean);
        
        try {
            parser.parseArgument(args);
            final AuthenticationResult authResult = new AuthenticationManager(new URI(optionsBean.endPoint),
                    optionsBean.username, optionsBean.password).login();
            if(authResult.hasError()){
                System.err.println("Could not authenticate: " + authResult.getError());
                System.exit(-1);
            }
            final UploadManager uploader = new UploadManager(authResult.getSessionId(), optionsBean.endPoint, optionsBean.projectName,
                    new File(optionsBean.zipPath));

            final UploaderResult result = uploader.uploadZip();

            if(result.hasError())
            	System.err.println(result.getError());
            else
            	System.out.println("projectId = " + result.getProjectId() + ", version = " + result.getVersion());
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }
}

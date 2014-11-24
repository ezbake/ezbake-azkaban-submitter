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

package ezbake.azkaban.submitter;

import com.google.common.base.Strings;
import ezbake.azkaban.manager.result.UploaderResult;
import ezbake.configuration.EzConfiguration;
import ezbake.configuration.EzConfigurationLoaderException;
import ezbake.configuration.constants.EzBakePropertyConstants;
import ezbakehelpers.ezconfigurationhelpers.azkaban.AzkabanConfigurationHelper;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.TException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

public class AzkabanSubmitter {

    protected String azkabanUrl;
    protected String azkabanPassword;
    protected String azkabanUsername;

    public AzkabanSubmitter() throws EzConfigurationLoaderException {
        this(new EzConfiguration().getProperties());
    }

    public AzkabanSubmitter(Properties properties){
        final AzkabanConfigurationHelper configurationHelper = new AzkabanConfigurationHelper(properties);
        azkabanUrl = configurationHelper.getAzkabanUrl();
        azkabanPassword = configurationHelper.getPassword();
        azkabanUsername = configurationHelper.getUsername();
    }

	@Option(name="-z", aliases="--pathToTarGz", usage="Path to the .tar.gz file being uploaded")
    String pathToTarGz;

    @Option(name="-p", aliases="--projectId", usage="Project ID")
    String projectId;

    @Option(name="-s", aliases="--securityId", usage="The security ID to use when creating an SSL connection to the submitter service", required=true)
    String securityId;

    @Option(name="-u", aliases="--submit", usage="Denotes that this will be a submit request")
    boolean submit = false;

    public UploaderResult submit(ByteBuffer zip, String projectName) {
        throw new NotImplementedException();
    }

    private void run(CmdLineParser parser) throws TException, IOException, CmdLineException {
        if (!(submit)) {
            throw new CmdLineException(parser, "Must provide -u option to client");
        }

        final EzConfiguration config;
        try {
            config = new EzConfiguration();
        } catch (EzConfigurationLoaderException e) {
            throw new IOException(e);
        }

        config.getProperties().setProperty(EzBakePropertyConstants.EZBAKE_SECURITY_ID, securityId);

        if (submit) {
            if (Strings.isNullOrEmpty(projectId)) throw new CmdLineException(parser, "Pipeline ID required for submission");
            File zipFile = new File(pathToTarGz);
            byte[] fileBytes = FileUtils.readFileToByteArray(zipFile);
            UploaderResult result = submit(ByteBuffer.wrap(fileBytes), projectId);
            System.out.println("Upload " + (result.hasError() ? "FAILED" : "SUCCESSFUL"));
        }
    }
    
    public static void main(String[] args) throws Exception {
        final AzkabanSubmitter client = new AzkabanSubmitter();
        final CmdLineParser parser = new CmdLineParser(client);

        try {
            parser.parseArgument(args);
            client.run(parser);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }
}

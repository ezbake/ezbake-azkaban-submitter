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

package ezbake.azkaban.submitter.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileUtil {

    /**
     * Saves the ByteBuffer as a temporary file in the default location
     *
     * @param byteBuffer The buffer to write to file
     * @param extension Any extension the tmp file should have
     * @return A {@link java.io.File} referencing the temporary file that was created
     * @throws IOException
     */
	public static File saveAsTempFile(ByteBuffer byteBuffer, String extension) throws IOException {
		final File tempFile = File.createTempFile("tmp", extension);
		try(FileOutputStream os = new FileOutputStream(tempFile)) {
			final FileChannel channel = os.getChannel();
			channel.write(byteBuffer);
			channel.force(true);
		}
		return tempFile;
	}

}

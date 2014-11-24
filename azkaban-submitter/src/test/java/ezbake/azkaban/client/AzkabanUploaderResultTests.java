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

package ezbake.azkaban.client;

import ezbake.azkaban.manager.result.UploaderResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class AzkabanUploaderResultTests {

	@Test
	public void testDeserializationFromJson() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			
			String json = "{ \"error\": \"error\", \"projectId\": 2, \"version\": 1}";
			UploaderResult result = mapper.readValue(json, UploaderResult.class);
			Assert.assertNotNull(result);
			Assert.assertEquals("error", result.getError());
			Assert.assertEquals(2, result.getProjectId());
			Assert.assertEquals(1, result.getVersion());
			
			json = "{ \"projectId\": 2, \"version\": 1}";
			result = mapper.readValue(json, UploaderResult.class);
			Assert.assertNotNull(result);
			Assert.assertNull(result.getError());
			Assert.assertEquals(2, result.getProjectId());
			Assert.assertEquals(1, result.getVersion());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.assertTrue(e.getMessage(), false);
		}
	}
}

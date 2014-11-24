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

import ezbake.azkaban.manager.result.AuthenticationResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class AzkabanAuthenticatorResultTests {

	@Test
	public void testDeserializationFromJson() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			
			String json = "{ \"error\": \"error\", \"session.id\": \"e7a29776-5783-49d7-afa0-b0e688096b5e\"}";
			AuthenticationResult result = mapper.readValue(json, AuthenticationResult.class);
			Assert.assertNotNull(result);
			Assert.assertEquals("error", result.getError());
			Assert.assertEquals("e7a29776-5783-49d7-afa0-b0e688096b5e", result.getSessionId());
			
			json = "{ \"session.id\": \"e7a29776-5783-49d7-afa0-b0e688096b5e\"}";
			result = mapper.readValue(json, AuthenticationResult.class);
			Assert.assertNotNull(result);
			Assert.assertNull(result.getError());
			Assert.assertEquals("e7a29776-5783-49d7-afa0-b0e688096b5e", result.getSessionId());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.assertTrue(e.getMessage(), false);
		}
	}
}

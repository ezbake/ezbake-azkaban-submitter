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

package ezbake.azkaban.client.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpManager {

	private static final PoolingHttpClientConnectionManager cm;
	static {
		try {
			//FIXME: comment this out in production
			SSLContextBuilder builder = SSLContexts.custom();
			builder.loadTrustMaterial(null, new TrustStrategy() {
			    @Override
			    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			        return true;
			    }
			});
			SSLContext sslContext = builder.build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

				@Override
				public boolean verify(String host, SSLSession sslSession) {
					return true;
				}

				@Override
				public void verify(String host, SSLSocket ssl) throws IOException { }

				@Override
				public void verify(String host, X509Certificate cert) throws SSLException { }

				@Override
				public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException { } 
				
			});

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
			        .<ConnectionSocketFactory> create().register("https", sslsf)
			        .build();
			
			cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			// Increase max total connection to 200
			cm.setMaxTotal(200);
			// Increase default max connection per route to 20
			cm.setDefaultMaxPerRoute(20);
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private HttpManager() { }
	
	public static HttpClient getClient() {
		return HttpClients
				.custom()
				.setConnectionManager(cm)
				.build();
	}

	public static String post(HttpPost post) throws Exception {
        final HttpClient client = HttpManager.getClient();
        final HttpResponse response = client.execute(post);
        final HttpEntity entity = response.getEntity();

        return EntityUtils.toString(entity);
	}

    public static String get(HttpGet get) throws Exception {
        final HttpClient client = HttpManager.getClient();
        final HttpResponse response = client.execute(get);
        final HttpEntity entity = response.getEntity();

        return EntityUtils.toString(entity);
    }

}

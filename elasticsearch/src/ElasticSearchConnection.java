/*******************************************************************************
 * Copyright 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ElasticSearchConnection {
	private Client client;
	private String index;
	private String hostname;
	private int port;
	private boolean connected = false;

	public ElasticSearchConnection() {
		// use defaults
		index = "healthcenter";
		hostname = "localhost";
		port = 9300;
		setupELK();
	}

	public ElasticSearchConnection(String index, String hostname, int port) {
		this.index = index;
		this.hostname = hostname;
		this.port = port;
		setupELK();
	}

	private void setupELK() {
		try {
			client = TransportClient
					.builder()
					.build()
					.addTransportAddress(
							new InetSocketTransportAddress(InetAddress
									.getByName(getHostname()), getPort()));
			connected = true;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		if (isConnected()) {
			try {
				setIndex();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("unable to find elasticsearch server on host "
					+ hostname + " on port " + port);
		}

	}

	private void setIndex() throws IOException, InterruptedException {
		IndicesExistsResponse res = getClient().admin().indices()
				.prepareExists(getIndex()).get();
		if (!res.isExists()) {
			System.out.println("preparing index as index does not exist");
			getClient().admin().indices().prepareCreate(getIndex()).get();
		} else {
			// System.out.println("Index already exists.");
		}
	}

	/*
	 * Put the mappings for the data we create into the index. It shouldn't
	 * matter if we replace existing records as they should be the same...
	 */

	public void putMappings(String fileName, String type) {

		JSONParser parser = new JSONParser();

		try {
			Object obj = parser
					.parse(new InputStreamReader(ElasticSearchConnection.class
							.getResourceAsStream(fileName)));

			JSONObject jsonObject = (JSONObject) obj;
			PutMappingResponse response = getClient().admin().indices()
					.preparePutMapping(getIndex()).setType(type)
					.setSource(jsonObject.toJSONString()).execute().actionGet();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	public Client getClient() {
		return client;
	}

	public void shutdown() {
		client.close();
		client = null;
	}

	public String getIndex() {
		return index;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public boolean isConnected() {
		return connected;
	}

}

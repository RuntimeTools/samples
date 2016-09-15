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

import org.json.simple.JSONObject;

import com.ibm.java.diagnostics.healthcenter.api.ConnectionProperties;
import com.ibm.java.diagnostics.healthcenter.api.HealthCenter;
import com.ibm.java.diagnostics.healthcenter.api.HealthCenterException;
import com.ibm.java.diagnostics.healthcenter.api.HealthCenterJMXException;
import com.ibm.java.diagnostics.healthcenter.api.HealthCenterSSLException;
import com.ibm.java.diagnostics.healthcenter.api.factory.HealthCenterFactory;

public class ESDemo {

	private HealthCenter hcAPI = null;
	private ConnectionProperties conn1;
	private ElasticSearchConnection eSC;
	private final String PORT = "-port"; //$NON-NLS-1$
	private final String MQTT = "-mqtt"; //$NON-NLS-1$
	private final String HOSTNAME = "-hostname"; //$NON-NLS-1$
	private final String ESPORT = "-esport"; //$NON-NLS-1$
	private final String ESHOSTNAME = "-eshostname"; //$NON-NLS-1$
	private int agentPortNum = 1972;
	private String agentHostName = "localhost";
	private int esPortNum = 9300;
	private String esHostName = "localhost";
	private boolean useMQTT = false;

	public static void main(String[] args) {
		new ESDemo(args);
	}

	ESDemo(String args[]) {

		// parse any command line overrides
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (PORT.equals(arg)) {
				i++;
				String nextArg = args[i];
				agentPortNum = Integer.parseInt(nextArg);
			} else if (HOSTNAME.equals(arg)) {
				i++;
				String nextArg = args[i];
				agentHostName = nextArg;
			} else if (ESHOSTNAME.equals(arg)) {
				i++;
				String nextArg = args[i];
				esHostName = nextArg;
			} else if (ESPORT.equals(arg)) {
				i++;
				String nextArg = args[i];
				esPortNum = Integer.parseInt(nextArg);
			} else if (MQTT.equals(arg)) {
				useMQTT = true;
			}
		}

		// Setup the connection to ElasticSearch
		eSC = new ElasticSearchConnection("healthcenter", esHostName, esPortNum);
		if (eSC.isConnected()) {
			// send the mappings file to use
			eSC.putMappings("gc.json", "gc");
			eSC.putMappings("cpu.json", "cpu");
			eSC.putMappings("memory.json", "memory");
			analyseLiveEventOnly();
		}
	}

	private boolean connectToAgent() {
		try {
			conn1 = new ConnectionProperties(agentHostName, agentPortNum);
			if (useMQTT) {
				conn1.setMQTTConnection();
			}
			hcAPI = HealthCenterFactory.connect(conn1);
			return true;
		} catch (HealthCenterJMXException e) {
			e.printStackTrace();
		} catch (HealthCenterSSLException e) {
			e.printStackTrace();
		} catch (HealthCenterException e) {
			System.out.println("Connection to agent failed, hostname "
					+ agentHostName + " port " + agentPortNum);
		}
		return false;
	}

	private void analyseLiveEventOnly() {
		try {
			if (connectToAgent()) {
				hcAPI.setEventOnlyMode(true);
				hcAPI.getPreferences().useBackingStorage(false);

				MyGCListener MyGC = new MyGCListener(eSC, intialiseJSONObject());
				hcAPI.getGCData().addGCListener(MyGC);

				MyCpuListener MyCPU = new MyCpuListener(eSC,
						intialiseJSONObject());
				hcAPI.getCpuData().addCpuListener(MyCPU);

				MyMemoryListener MyMEM = new MyMemoryListener(eSC,
						intialiseJSONObject());
				hcAPI.getNativeMemoryData().addNativeMemoryListener(MyMEM);

				System.out.println("press a key to exit monitoring");
				System.in.read();

				hcAPI.endMonitoring();
			}

			eSC.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject intialiseJSONObject() {
		if (!(hcAPI.getEnvironmentData().getProcessId() > 0)) {
			System.out.print("initialising data .");
			while (!(hcAPI.getEnvironmentData().getProcessId() > 0)) {
				try {
					System.out.print(".");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println(" initialisation complete");
		}

		JSONObject obj = new JSONObject();
		obj.put("hostName", hcAPI.getEnvironmentData().getHostName());
		obj.put("pid", hcAPI.getEnvironmentData().getProcessId());
		obj.put("commandLine", hcAPI.getEnvironmentData().getJavaCommandLine()
				.toString());
		obj.put("agentPort", agentPortNum);
		return obj;
	}

}

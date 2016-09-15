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

import org.elasticsearch.action.index.IndexResponse;
import org.json.simple.JSONObject;

import com.ibm.java.diagnostics.healthcenter.api.nativememory.NativeMemoryEvent;
import com.ibm.java.diagnostics.healthcenter.api.nativememory.NativeMemoryEventListener;

class MyMemoryListener implements NativeMemoryEventListener {

	private ElasticSearchConnection eSC;
	private JSONObject obj;

	public MyMemoryListener(ElasticSearchConnection eSC, JSONObject obj) {
		super();
		this.eSC = eSC;
		this.obj = obj;
	}

	@SuppressWarnings("unchecked")
	public void nativeMemoryEvent(NativeMemoryEvent event) {
		obj.put("timestamp", event.getEventTime());
		JSONObject processMem = new JSONObject();
		processMem.put("private", event.getProcessPrivate());
		processMem.put("physical", event.getProcessPhysical());
		processMem.put("virtual", event.getProcessVirtual());

		JSONObject systemMem = new JSONObject();
		systemMem.put("physical", event.getFreePhysicalMemory());
		systemMem.put("total", event.getTotalPhysicalMemory());

		JSONObject mem = new JSONObject();
		mem.put("process", processMem);
		mem.put("system", systemMem);
		obj.put("memory", mem);

		IndexResponse response = eSC.getClient()
				.prepareIndex(eSC.getIndex(), "memory")
				.setSource(obj.toJSONString()).get();
	}
}
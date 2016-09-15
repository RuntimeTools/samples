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

import com.ibm.java.diagnostics.healthcenter.api.gc.GCEvent;
import com.ibm.java.diagnostics.healthcenter.api.gc.GCEventListener;

class MyGCListener implements GCEventListener {

	private ElasticSearchConnection eSC;
	private JSONObject obj;

	public MyGCListener(ElasticSearchConnection eSC, JSONObject obj) {
		super();
		this.eSC = eSC;
		this.obj = obj;
	}

	@SuppressWarnings("unchecked")
	public void gcEvent(GCEvent event) {
		//System.out.println("I've been called in myGCListener");

		obj.put("timestamp", event.getEventTime());
		JSONObject gc = new JSONObject();
		gc.put("type", event.getType());
		gc.put("size", event.getHeapSize());
		gc.put("used", event.getUsedHeapAfterGC());
		gc.put("duration", event.getPauseTime());
		obj.put("gc", gc);

		IndexResponse response = eSC.getClient()
				.prepareIndex(eSC.getIndex(), "gc")
				.setSource(obj.toJSONString()).get();

	}

}
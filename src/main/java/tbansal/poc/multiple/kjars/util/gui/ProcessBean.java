/**
 * Copyright 2015, Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tbansal.poc.multiple.kjars.util.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.inject.Model;
import javax.inject.Inject;

import tbansal.poc.multiple.kjars.util.BootStrap;
import tbansal.poc.multiple.kjars.util.WorkFlowService;

@Model
public class ProcessBean {

	@EJB
	WorkFlowService workFlowService;

	@Inject
	Logger logger;

	private String recipient;
	private int reward = 200;

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getRecipient() {
		return recipient;
	}

	public int getReward() {
		return reward;
	}

	public void setReward(int reward) {
		this.reward = reward;
	}

	public String startProcess() {
		String message;
		long processInstanceId = -1;
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("recipient", recipient);
			params.put("reward", reward);
			processInstanceId = workFlowService.startProcess("org.jbpm.examples.rewards").getId();
			message = "Process instance " + processInstanceId + " has been successfully started.";
			logger.info(message);
		} catch (Exception e) {
			message = "Unable to start the business process.";
			logger.log(Level.SEVERE, message, e);
		}
		return "index.xhtml?faces-redirect=true";
	}
}

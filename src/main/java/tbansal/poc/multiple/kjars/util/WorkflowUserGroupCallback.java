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

package tbansal.poc.multiple.kjars.util;

import org.kie.api.task.UserGroupCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TB91535
 * 
 *         Dummy implementation for Task Service- Will not be used at runtime,
 *         as JBPM will delegate all the request to the custom task manager
 *
 */
public class WorkflowUserGroupCallback implements UserGroupCallback {

	public boolean existsUser(String userId) {
		return true;
	}

	public boolean existsGroup(String groupId) {
		return true;
	}

	public List<String> getGroupsForUser(String userId, List<String> groupIds, List<String> allExistingGroupIds) {
		List<String> groups = new ArrayList<String>();
		return groups;
	}
}

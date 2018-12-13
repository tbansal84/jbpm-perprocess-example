package tbansal.poc.multiple.kjars.util;

import java.util.Collections;
import java.util.List;

import org.kie.internal.identity.IdentityProvider;

/**
 * @author TB91535
 * 
 *         Dummy implementation for Task Service- Will not be used at runtime,
 *         as JBPM will delegate all the request to the custom task manager
 *
 */
public class WorkflowIdentityProvider implements IdentityProvider {

	private static final String SYSTEM_USER = "system";

	public String getName() {

		return SYSTEM_USER;
	}

	public List<String> getRoles() {

		return Collections.emptyList();
	}

	public boolean hasRole(String role) {
		return true;
	}

}
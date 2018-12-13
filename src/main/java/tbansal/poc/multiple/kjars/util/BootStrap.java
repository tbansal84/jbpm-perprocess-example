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

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;

import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.services.task.audit.JPATaskLifeCycleEventListener;
import org.kie.api.task.TaskLifeCycleEventListener;

@ApplicationScoped
public class BootStrap {

	@PersistenceUnit(unitName = "org.jbpm.domain")
	private EntityManagerFactory emf;

	@Produces
	public EntityManagerFactory produceEntityManagerFactory() {
		if (this.emf == null) {
			this.emf = Persistence.createEntityManagerFactory("org.jbpm.domain");
		}
		return this.emf;
	}

	@Inject
	private WorkflowDeploymentService deploymentService;

	@Produces
	public DeploymentService produceDeploymentService() {
//         return deploymentService.select(new AnnotationLiteral<Kjar>() {}).get();
		return null;
	}

	@Produces
	public TaskLifeCycleEventListener produceAuditListener() {
		return new JPATaskLifeCycleEventListener(true);
	}

	public static final String DEPLOYMENT_ID = "org.jbpm.examples:rewards:1.0";

	@PostConstruct
	public void init() {
		String[] gav = DEPLOYMENT_ID.split(":");
		DeploymentUnit deploymentUnit = new KModuleDeploymentUnit(gav[0], gav[1], gav[2]);
		deploymentService.deploy(deploymentUnit);
	}

	@Produces
	@PropertiesResource(name = "", loader = "")
	Properties loadProperties(InjectionPoint ip) {
		System.out.println("-- called PropertiesResource loader");
		PropertiesResource annotation = ip.getAnnotated().getAnnotation(PropertiesResource.class);
		String fileName = annotation.name();
		String loader = annotation.loader();
		Properties props = null;
		// Load the properties from file
		URL url = null;
		url = Thread.currentThread().getContextClassLoader().getResource(fileName);
		if (url != null) {
			props = new Properties();
			try {
				props.load(url.openStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return props;
	}

}

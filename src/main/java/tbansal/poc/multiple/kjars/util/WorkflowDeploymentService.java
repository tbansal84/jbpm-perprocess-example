/*
s * Copyright 2014 Red Hat, Inc. and/or its affiliates.
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

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ejb.Stateless;

import org.drools.compiler.kie.builder.impl.ClasspathKieProject;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.KieContainerImpl;
import org.drools.compiler.kie.builder.impl.KieModuleKieProject;
import org.drools.compiler.kie.builder.impl.KieProject;
import org.drools.compiler.kie.builder.impl.KieRepositoryImpl;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.drools.compiler.kproject.xml.PomModel;
import org.drools.core.util.StringUtils;
import org.jbpm.kie.services.impl.DeployedUnitImpl;
import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.bpmn2.ProcessDescriptor;
import org.jbpm.process.audit.event.AuditEventBuilder;
import org.jbpm.services.api.model.DeploymentUnit;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author TB91535
 * 
 *         This class will work as EPO custom Kmodule deployment service.
 * 
 *         To be modified to enable deployment of multiple kjars.
 *
 */
@Stateless
public class WorkflowDeploymentService extends KModuleDeploymentService {

	private static Logger logger = LoggerFactory.getLogger(WorkflowDeploymentService.class);
	private static final String DEFAULT_KBASE_NAME = "defaultKieBase";

	public List<String> deployKModule(DeploymentUnit unit) {
		List<String> processes = null;

		try {
			if (deploymentsMap.containsKey(unit.getIdentifier())) {
				throw new IllegalStateException("Unit with id " + unit.getIdentifier() + " is already deployed");
			}
			if (!(unit instanceof KModuleDeploymentUnit)) {
				throw new IllegalArgumentException("Invalid deployment unit provided - " + unit.getClass().getName());
			}
			KModuleDeploymentUnit kmoduleUnit = (KModuleDeploymentUnit) unit;
			DeployedUnitImpl deployedUnit = new DeployedUnitImpl(unit);
//			deployedUnit.setActive(kmoduleUnit.isActive());

			// Create the release id
			KieContainer kieContainer = kmoduleUnit.getKieContainer();
			ReleaseId releaseId = null;
			if (kieContainer == null) {
				KieServices ks = KieServices.Factory.get();

				releaseId = ks.newReleaseId(kmoduleUnit.getGroupId(), kmoduleUnit.getArtifactId(),
						kmoduleUnit.getVersion());

//				MavenRepository repository = getMavenRepository();
//				repository.resolveArtifact(releaseId.toExternalForm());

				kieContainer = newKieContainer(releaseId);

				kmoduleUnit.setKieContainer(kieContainer);
			}
			releaseId = kieContainer.getReleaseId();

			// retrieve the kbase name
			String kbaseName = kmoduleUnit.getKbaseName();
			if (StringUtils.isEmpty(kbaseName)) {
				KieBaseModel defaultKBaseModel = ((KieContainerImpl) kieContainer).getKieProject()
						.getDefaultKieBaseModel();
				if (defaultKBaseModel != null) {
					kbaseName = defaultKBaseModel.getName();
				} else {
					kbaseName = DEFAULT_KBASE_NAME;
				}
			}
			InternalKieModule module = (InternalKieModule) ((KieContainerImpl) kieContainer)
					.getKieModuleForKBase(kbaseName);
			if (module == null) {
				throw new IllegalStateException(
						"Cannot find kbase, either it does not exist or there are multiple default kbases in kmodule.xml");
			}

			KieBase kbase = kieContainer.getKieBase(kbaseName);
			Map<String, ProcessDescriptor> processDescriptors = new HashMap<String, ProcessDescriptor>();
			for (org.kie.api.definition.process.Process process : kbase.getProcesses()) {
				processDescriptors.put(process.getId(),
						(ProcessDescriptor) process.getMetaData().get("ProcessDescriptor"));
			}

			// TODO: add forms data?
			Collection<String> files = module.getFileNames();

			processResources(module, files, kieContainer, kmoduleUnit, deployedUnit, releaseId, processDescriptors);
			processes = files.stream().filter(fileName -> fileName.matches(".+bpmn[2]?$"))
					.map(fileName -> getProcessId(fileName)).collect(Collectors.toList());

			// process the files in the deployment
			if (module.getKieDependencies() != null) {
				Collection<InternalKieModule> dependencies = module.getKieDependencies().values();
				for (InternalKieModule depModule : dependencies) {

					logger.debug("Processing dependency module " + depModule.getReleaseId());
					files = depModule.getFileNames();

					processResources(depModule, files, kieContainer, kmoduleUnit, deployedUnit,
							depModule.getReleaseId(), processDescriptors);
				}
			}
//			Collection<ReleaseId> dependencies = module
//					.getJarDependencies(new DependencyFilter.ExcludeScopeFilter("test", "provided"));

			// process deployment dependencies
//			if (dependencies != null && !dependencies.isEmpty()) {
//				// Classes 2: classes added from project and dependencies added
//				processClassloader(kieContainer, deployedUnit);
//			}

			AuditEventBuilder auditLoggerBuilder = setupAuditLogger(identityProvider, unit.getIdentifier());

			RuntimeEnvironmentBuilder builder = boostrapRuntimeEnvironmentBuilder(kmoduleUnit, deployedUnit,
					kieContainer, kmoduleUnit.getMergeMode()).knowledgeBase(kbase)
							.classLoader(kieContainer.getClassLoader());

			builder.registerableItemsFactory(
					getRegisterableItemsFactory(auditLoggerBuilder, kieContainer, kmoduleUnit));

			commonDeploy(unit, deployedUnit, builder.get(), kieContainer);
			kmoduleUnit.setDeployed(true);
		} catch (Throwable e) {
			logger.warn("Unexpected error while deploying unit {}", unit.getIdentifier(), e);
			// catch all possible errors to be able to report them to caller as
			// RuntimeException
			throw new RuntimeException(e);
		}

		return processes;

	}

	protected KieContainer newKieContainer(ReleaseId releaseId) {
		logger.debug("Loading custom container");
		InternalKieModule kieModule = (InternalKieModule) getRepository().getKieModule(releaseId);
		if (kieModule == null) {
			throw new RuntimeException("Cannot find KieModule: " + releaseId);
		}
		KieProject kProject = new KieModuleKieProject(kieModule, findParentClassLoader());

		KieContainerImpl newContainer = new KieContainerImpl(UUID.randomUUID().toString(), kProject, getRepository(),
				releaseId);
		return newContainer;
	}

	private ClassLoader findParentClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}

	protected KieRepository getRepository() {
		logger.debug("Loading from Custom Repository");
		return new KieRepositoryImpl() {

			public KieModule getKieModule(ReleaseId releaseId, PomModel pomModel) {
				KieModule kieModule = null;
				if (kieModule == null) {
					logger.debug("KieModule Lookup. ReleaseId {} was not in cache, checking classpath",
							releaseId.toExternalForm());
					kieModule = checkClasspathForKieModule(releaseId);
				}

				return kieModule;
			}

			private KieModule checkClasspathForKieModule(ReleaseId releaseId) {
				logger.debug("Loading the kmodule from classpath {}", releaseId.toExternalForm());
				ClassLoader classLoader = findParentClassLoader();
				URL pathToKmodule = classLoader
						.getResource(((ReleaseIdImpl) releaseId).getCompilationCachePathPrefix() + "kmodule.xml");
				return ClasspathKieProject.fetchKModule(pathToKmodule);
			}

		};
	}

}

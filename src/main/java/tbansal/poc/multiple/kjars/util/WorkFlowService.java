package tbansal.poc.multiple.kjars.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.manager.context.EmptyContext;

@Stateless
public class WorkFlowService {

	@EJB
	WorkflowDeploymentService workflowDeploymentService;

	@EJB
	KieModuleDiscoveryService kieModuleDiscoveryService;

	Map<String, ReleaseId> units = new HashMap<>();

	private ClassLoader getClassLoader() {
		return WorkFlowService.class.getClassLoader();
	}

	@PostConstruct
	public void init() {

		List<ReleaseId> modules = kieModuleDiscoveryService.discoverKieModules(getClassLoader());

		modules.stream().forEach(unit -> workflowDeploymentService
				.deployKModule(new KModuleDeploymentUnit(unit.getGroupId(), unit.getArtifactId(), unit.getVersion()))
				.stream().forEach(processName -> storeAssetVersions(processName, unit)));

		;

	}

	private void storeAssetVersions(String processName, ReleaseId unitId) {
		ReleaseId id = units.putIfAbsent(processName, unitId);
		if (Integer.valueOf(id.getVersion()) > Integer.valueOf(unitId.getVersion())) {
			units.put(processName, id);
		}
	}

	public ProcessInstance startProcess(String processId) {
		ProcessInstance deploymentId = workflowDeploymentService
				.getRuntimeManager(units.get(processId).toExternalForm()).getRuntimeEngine(EmptyContext.get())
				.getKieSession().startProcess(processId);
		return deploymentId;

	}
}

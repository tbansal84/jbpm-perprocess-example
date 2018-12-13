package tbansal.poc.multiple.kjars.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import org.drools.compiler.kie.builder.impl.ClasspathKieProject;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kproject.models.KieModuleModelImpl;
import org.kie.api.builder.ReleaseId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class KieModuleDiscoveryService {

	private static Logger logger = LoggerFactory.getLogger(WorkflowDeploymentService.class);
	@Resource(name = "groupId")
	private String groupId;
	@Resource(name = "artifactIds")
	private String artifactIds;

	public Map<String, ReleaseId> listeDossierKieModules(ClassLoader classLoader) {

		List<ReleaseId> allKieModulesInClasspath = discoverKieModules(classLoader);
		Map<String, ReleaseId> map = (Map<String, ReleaseId>) allKieModulesInClasspath.stream()
				.filter(m -> m.getGroupId().equals(groupId) && artifactIds.contains(m.getArtifactId()))
				.collect(Collectors.toMap(x -> x.getArtifactId(), x -> x, (x1, x2) -> {
					logger.warn("Duplicate versions {} -- {} found for artifact {} ", x1.getVersion(), x2.getVersion(),
							x1.getArtifactId());
					return Integer.valueOf(x1.getVersion()) > Integer.valueOf(x2.getVersion()) ? x1 : x2;
				}));

		return map;

	}

	public List<ReleaseId> discoverKieModules(ClassLoader classLoader) {
		List<ReleaseId> allKieModulesInClasspath = new ArrayList<ReleaseId>();

		String[] configFiles = { KieModuleModelImpl.KMODULE_JAR_PATH, KieModuleModelImpl.KMODULE_SPRING_JAR_PATH };
		for (String configFile : configFiles) {
			Enumeration<URL> e = null;
			try {
				e = classLoader.getResources(configFile);
			} catch (IOException exc) {
				logger.error("Unable to find and build index of " + configFile + "." + exc.getMessage());
			}

			// Map of kmodule urls
			while (e != null && e.hasMoreElements()) {
				URL url = e.nextElement();
				try {
					InternalKieModule kModule = ClasspathKieProject.fetchKModule(url);

					if (kModule != null) {
						ReleaseId releaseId = kModule.getReleaseId();
						allKieModulesInClasspath.add(releaseId);

						logger.debug("Discovered classpath module " + releaseId.toExternalForm());

					}

				} catch (Exception exc) {
					logger.error("Unable to build index of kmodule.xml url=" + url.toExternalForm() + "\n"
							+ exc.getMessage());
				}
			}
		}

		return allKieModulesInClasspath.stream()
				.filter(m -> m.getGroupId().equals(groupId) && artifactIds.contains(m.getArtifactId()))
				.collect(Collectors.toList());
	}

}

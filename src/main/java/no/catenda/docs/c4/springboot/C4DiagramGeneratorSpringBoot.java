package no.catenda.docs.c4.springboot;

import com.structurizr.Workspace;
import com.structurizr.model.Container;
import com.structurizr.model.Person;
import com.structurizr.model.SoftwareSystem;
import no.catenda.docs.c4.C4DiagramGeneratorParent;
import no.catenda.docs.c4.ComponentFindingConfiguration;

public class C4DiagramGeneratorSpringBoot extends C4DiagramGeneratorParent {

  C4DiagramGeneratorSpringBoot(String folderWithCompiledClasses, String basePackageForScanning,
      String outputFolder, ComponentFindingConfiguration configuration) {
    super(folderWithCompiledClasses, basePackageForScanning, outputFolder, configuration);
  }

  public static void main(String[] args) throws Exception {
    var folderWithCompiledClasses = args.length > 0 ? args[0]
        : "/Users/tom-czerwinski-catenda/tk-tests/test-component86/test-component-service/target/classes";
    var basePackageForScanning = args.length > 1 ? args[1] : DEFAULT_BASE_PACKAGE;
    var outputFolder = args.length > 1 ? args[2] : DEFAULT_OUTPUT_FOLDER;
    var configuration = ComponentFindingConfiguration.builder()
        .registerDefaultC4ExternalSystemAdapterAnnotationFinder(true)
        .namingStrategy(DEFAULT_COMPONENT_NAMING_STRATEGY.get())
        .build()
        .addStrategy("org.springframework.web.bind.annotation.RestController",
            ComponentType.INTERFACES_PUBLIC_API)
        .addStrategy("org.springframework.stereotype.Service", ComponentType.DOMAIN_SERVICE);

    new C4DiagramGeneratorSpringBoot(folderWithCompiledClasses, basePackageForScanning,
        outputFolder, configuration)
        .generateC4Diagram("Test system/product", "Test container");
  }

  public void addC4ElementsToWorkspace(Workspace w, String systemName, String containerName) {

    // Level 1: System Context (Software Systems + People)
    Person user = w.getModel().addPerson("User", "An authenticated user of the system.");

    // this system is the system/product in which context are we in right now
    SoftwareSystem thisSystem = w.getModel().addSoftwareSystem(
        systemName,
        "Handles entity lookup/creation and exposes a REST API.");

    // Level 2: Containers (Subparts of a Software System - not docker-containers)
    Container thisService = thisSystem.addContainer(containerName,
        "Manages MainEntity",
        "Spring Boot");

    // Level 3: Components + ExternalSystems
    discoverLevel3ElementsAndExternalSystems(w, thisService);

    // Level 1&2&2 - mark all as used by the user
    createRelationFromTheUserToAllRestApis(thisService, user);
  }
}

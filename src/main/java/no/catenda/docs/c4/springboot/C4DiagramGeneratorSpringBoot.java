package no.catenda.docs.c4.springboot;

import com.structurizr.Workspace;
import com.structurizr.model.Container;
import com.structurizr.model.Person;
import com.structurizr.model.SoftwareSystem;
import no.catenda.docs.c4.C4DiagramGeneratorParent;
import no.catenda.docs.c4.C4DiagramGeneratorStarter;
import no.catenda.docs.c4.ComponentFindingConfiguration;

public class C4DiagramGeneratorSpringBoot extends C4DiagramGeneratorParent {

  public C4DiagramGeneratorSpringBoot(String folderWithCompiledClasses,
      String basePackageForScanning,
      String outputFolder, ComponentFindingConfiguration configuration) {
    super(folderWithCompiledClasses, basePackageForScanning, outputFolder, configuration);
  }

  static void main(String[] args) throws Exception {
    C4DiagramGeneratorStarter.startSpringBoot("Test system/product", "Test container");
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

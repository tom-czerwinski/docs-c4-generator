package no.catenda.docs.c4.dropwizard;

import com.structurizr.Workspace;
import com.structurizr.component.ComponentFinderBuilder;
import com.structurizr.component.naming.FullyQualifiedNamingStrategy;
import com.structurizr.model.Container;
import com.structurizr.model.Person;
import com.structurizr.model.SoftwareSystem;
import no.catenda.docs.c4.C4DiagramGeneratorParent;
import no.catenda.docs.c4.C4ExternalSystemAdapter;
import no.catenda.docs.c4.ComponentFindingConfiguration;

public class C4DiagramGeneratorBimsync extends C4DiagramGeneratorParent {

  C4DiagramGeneratorBimsync(String folderWithCompiledClasses, String basePackageForScanning,
      String outputFolder, ComponentFindingConfiguration configuration) {
    super(folderWithCompiledClasses, basePackageForScanning, outputFolder, configuration);
  }

  public static void main(String[] args) throws Exception {
    var folderWithCompiledClasses = args.length > 0 ? args[0] : "/Users/tom-czerwinski-catenda/IdeaProjects/bimsync/bimsync-arena-edge/target";
    var basePackageForScanning = args.length > 1 ? args[1] : DEFAULT_BASE_PACKAGE;
    var outputFolder = args.length > 1 ? args[2] : DEFAULT_OUTPUT_FOLDER;
    var configuration = ComponentFindingConfiguration.builder()
        .registerDefaultC4ExternalSystemAdapterAnnotationFinder(true)
        .namingStrategy(new FullyQualifiedNamingStrategy())
        .build()
        .addStrategy("javax.ws.rs.Path", ComponentType.INTERFACES_PUBLIC_API);


    new C4DiagramGeneratorBimsync(folderWithCompiledClasses, basePackageForScanning, outputFolder, configuration)
        .generateC4Diagram("Test system/product", "Test container");
  }

  public void addC4ElementsToWorkspace(Workspace w, String systemName, String containerName) {

    // Level 1: System Context (Software Systems + People)
    Person user = w.getModel().addPerson("User", "An authenticated user of the system.");

    // this system is the system/product in which context are we in right now
    SoftwareSystem thisSystem = w.getModel().addSoftwareSystem(systemName, "Handles EVERYTHING");

    // Level 2: Containers (Subparts of a Software System - not docker-containers)
    Container thisService =
        thisSystem.addContainer(containerName, "Manages EVERYTHING", "Dropwizard");

    // Level 3: Components + ExternalSystems
    discoverLevel3ElementsAndExternalSystems(w, thisService);

    // Level 1&2&2 - mark all as used by the user
    createRelationFromTheUserToAllRestApis(thisService, user);
  }
}

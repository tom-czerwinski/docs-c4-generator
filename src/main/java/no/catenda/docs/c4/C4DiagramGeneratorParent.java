package no.catenda.docs.c4;

import static java.util.stream.Collectors.counting;

import com.structurizr.Workspace;
import com.structurizr.component.ComponentFinderBuilder;
import com.structurizr.component.ComponentFinderStrategy;
import com.structurizr.component.ComponentFinderStrategyBuilder;
import com.structurizr.component.matcher.AnnotationTypeMatcher;
import com.structurizr.component.naming.NamingStrategy;
import com.structurizr.component.naming.TypeNamingStrategy;
import com.structurizr.component.supporting.DefaultSupportingTypesStrategy;
import com.structurizr.export.plantuml.StructurizrPlantUMLExporter;
import com.structurizr.model.Component;
import com.structurizr.model.Container;
import com.structurizr.model.Person;
import com.structurizr.model.SoftwareSystem;
import com.structurizr.model.Tags;
import com.structurizr.view.ComponentView;
import com.structurizr.view.ContainerView;
import com.structurizr.view.Shape;
import com.structurizr.view.Styles;
import com.structurizr.view.SystemContextView;
import com.structurizr.view.ViewSet;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class C4DiagramGeneratorParent {

  public static final String DEFAULT_OUTPUT_FOLDER = "target/docs-c4";

  public static final String DEFAULT_CLASSES_DIR = "target/classes";

  public static final String DEFAULT_BASE_PACKAGE = "no.catenda";

  public static final Supplier<NamingStrategy> DEFAULT_COMPONENT_NAMING_STRATEGY = () -> new TypeNamingStrategy();

  public C4DiagramGeneratorParent(String pathToJarOrFolderWithCompiledClasses,
      String basePackageForScanning,
      String outputFolder, ComponentFindingConfiguration componentFindingConfiguration) {
    this.basePackageForScanning = basePackageForScanning;
    this.pathToJarOrFolderWithCompiledClasses = pathToJarOrFolderWithCompiledClasses;
    this.outputFolder = outputFolder;
    this.componentFinderStrategyFactory = new ComponentFinderStrategyFactory(componentFindingConfiguration);
  }

  protected final String basePackageForScanning;
  protected final String pathToJarOrFolderWithCompiledClasses;
  protected final String outputFolder;
  protected final ComponentFinderStrategyFactory componentFinderStrategyFactory;

  private Map<String, C4ExternalSystemData> cachedC4ExternalSystems;
  private Map<String, List<String>> cachedClassInterfaces;
  private Map<String, List<String>> cachedClassFieldTypes;

  /**
   * Holds annotation data extracted from a class annotated with {@link C4ExternalSystemAdapter} or
   * one of its composed annotations (e.g. {@link C4ExternalSystemAdapter.Kafka}).
   */
  protected record C4ExternalSystemData(
      String className,
      String externalSystemName,
      String description,
      String relationship,
      String technology,
      String annotationType
  ) {

  }

  /**
   * Add C4 elements (Users, SoftwareSystems, Components, etc) to given workspace from which models
   * and views will be generated. Created elements MUST include SoftwareSystem with systemName and
   * its child-container with containerName. It needs to be provided explicitly by the service
   * developers.
   *
   * @param w             C4 Workspace that is an aggregate for all the model elements
   * @param systemName    Name of the whole system (/PRODUCT) that aggregates multiple services
   * @param containerName service/application name, called in C4 container
   */
  protected abstract void addC4ElementsToWorkspace(Workspace w, String systemName,
      String containerName);

  public void generateC4Diagram(String systemName, String containerName) throws Exception {
    //Level 0: Main aggregate for the whole design
    var workspace = new Workspace(systemName, "C4 diagrams for the " + systemName + ".");

    //Level 1 & 2 & 3 - done by abstract method
    addC4ElementsToWorkspace(workspace, systemName, containerName);

    //Crete renderable views
    createViews(workspace, workspace.getModel().getSoftwareSystemWithName(systemName)
        .getContainerWithName(containerName));

    exportToJson(workspace);
    exportToPlantUml(workspace);
  }

  /**
   * Automatically discover: Level 3: Components (Subparts of a Container) Level 1 & 2 & 3 -
   * External Systems
   */
  protected void discoverLevel3ElementsAndExternalSystems(Workspace w, Container thisService) {
    // Level 3: Components (Subparts of a Container)
    discoverInternalComponentsAndAddThemTo(thisService);

    // Level 1 & 2 & 3 - discover external systems from @C4ExternalSystem and wire to components
    discoverExternalSoftwareSystemsAndMarkTheirUsage(thisService.getSoftwareSystem(), thisService);
  }

  /**
   * Automatically discover Components and add them to the given service. Components = REST API,
   * domain-service, external adapter.
   */
  protected void discoverInternalComponentsAndAddThemTo(Container service) {

    var componentFinderBuilder = new ComponentFinderBuilder()
        .forContainer(service)
        .fromClasses(new File(pathToJarOrFolderWithCompiledClasses));

    //for each of the configured component-finder, create and register one
    componentFinderStrategyFactory.createConfiguredComponentFinders()
        .forEach(componentFinderBuilder::withStrategy);

    //TODO move to ComponentFinderStrategyFactory
    if(componentFinderStrategyFactory.getConfiguration().isRegisterDefaultC4ExternalSystemAdapterAnnotationFinder()){
      registerDefaultC4ExternalSystemAdapterAnnotationFinder(componentFinderBuilder);
    }

    log.info(
        "C4-docs generator - scanning classes based on registered finding-strategies (folder: '{}') - starting",
        pathToJarOrFolderWithCompiledClasses);

    //invoking scan
    componentFinderBuilder.build().run();

    if (log.isDebugEnabled()) {
      log.debug(
          "C4-docs generator - scanning classes - found {} components: {}",
          service.getComponents().size(),
          service.getComponents().stream()
              .collect(java.util.stream.Collectors.groupingBy(
                  Component::getTechnology,
                  counting())));
    } else {
      log.info(
          "C4-docs generator - scanning classes - found {} components: {}",
          service.getComponents().size(),
          service.getComponents().stream()
              .collect(java.util.stream.Collectors.groupingBy(
                  Component::getTechnology,
                  counting())));
    }

    log.info(
        "C4-docs generator - scanning classes based on registered finding-strategies (folder: '{}') - completed",
        pathToJarOrFolderWithCompiledClasses);

    // Assign groups so PlantUML clusters components by architectural layer
    groupComponentsForBetterVisualEffect(service);
  }

  /**
   * Add find-strategy for the {@link ComponentFinderBuilder} that will find and register a
   * C4-Component for each of the found class that is marked by the SpringFramework specific
   * annotation.
   */
  protected void registerSpringFrameworkClassFinder(ComponentFinderBuilder finderBuilder) {
    finderBuilder.withStrategy(
        componentFinderStrategyFactory.createComponentFinder(
            "org.springframework.web.bind.annotation.RestController",
            ComponentType.INTERFACES_PUBLIC_API)
    );
    finderBuilder.withStrategy(
        componentFinderStrategyFactory.createComponentFinder("org.springframework.stereotype.Service",
            ComponentType.DOMAIN_SERVICE)
    );
  }

  /**
   * Add find-strategy for the {@link ComponentFinderBuilder} that will find and register a
   * C4-Component for each of the found class that is marked by the {@link C4ExternalSystemAdapter}
   * annotation.
   */
  protected void registerDefaultC4ExternalSystemAdapterAnnotationFinder(
      ComponentFinderBuilder finderBuilder) {
    // Only register strategies for C4ExternalSystem annotation types actually used in the codebase
    findUsedC4ExternalSystemAnnotationTypes()
        //for each of the used C4ExternalSystem annotation, create
        .forEach(annotationType -> finderBuilder.withStrategy(
            componentFinderStrategyFactory.createComponentFinder(annotationType, ComponentType.INFRASTRUCTURE_ADAPTER)
        ));
  }

  protected ComponentFinderStrategy createComponentFinder(String annotationType,
      ComponentType infrastructureAdapter) {
    return componentFinderStrategyFactory.createComponentFinder(annotationType, infrastructureAdapter);
  }

  /**
   * Discover all the classes marked by the {@link C4ExternalSystemAdapter} and create connections
   * to them from our SoftwareSystem, Container and Components.
   */
  protected void discoverExternalSoftwareSystemsAndMarkTheirUsage(SoftwareSystem thisSystem,
      Container thisService) {

    for (var entry : findC4ExternalSystems().entrySet()) {
      String adapterClassName = entry.getKey();
      C4ExternalSystemData data = entry.getValue();

      // Level 1: register external system and create relation from our system to it
      SoftwareSystem externalSystem = thisSystem.getModel().addSoftwareSystem(
          data.externalSystemName(),
          data.description());
      thisSystem.uses(externalSystem, data.relationship(), data.technology());

      // Level 2: create relation from our service to external
      thisService.uses(externalSystem, data.relationship(), data.technology());

      // Level 3: create relations from adapter to external system and from internal component to adapter
      findComponentByType(thisService, adapterClassName)
          .ifPresent(adapterComponent -> {
            //mark that the adapter uses external system
            adapterComponent.uses(externalSystem, data.relationship(),
                data.technology());
            //mark all components that use the adapter
            markRelationsBetweenTheAdapterAndConsumersThatUseIt(thisService,
                adapterClassName, adapterComponent);
          });
    }
  }

  protected void createRelationFromTheUserToAllRestApis(Container service, Person user) {
    user.uses(service.getSoftwareSystem(), "Calls REST API", "HTTPS");
    user.uses(service, "Calls REST API", "HTTPS");
    for (Component component : service.getComponents()) {
      if (ComponentType.INTERFACES_PUBLIC_API.name().equals(component.getTechnology())) {
        user.uses(component, "Calls REST API", "HTTPS");
      }
    }
  }

  /**
   * Finds components that depend on this adapter through its domain interfaces (ports). The
   * ComponentFinder resolves direct class references but not interface-to-implementation
   * dependencies (e.g. MainService -> GreetingProvider -> GreetingServiceClient). Uses
   * bytecode-level inspection via ClassGraph (no classpath loading required).
   */
  protected void markRelationsBetweenTheAdapterAndConsumersThatUseIt(
      Container container, String adapterClassName, Component adapterComponent) {
    List<String> adapterInterfaces = getClassInterfaces(adapterClassName);
    for (String ifaceName : adapterInterfaces) {
      for (Component consumer : container.getComponents()) {
        if (consumer == adapterComponent) {
          continue;
        }
        String consumerType = consumer.getProperties().get("component.type");
        if (consumerType == null) {
          continue;
        }
        for (String fieldTypeName : getClassFieldTypes(consumerType)) {
          if (fieldTypeName.equals(ifaceName)) {
            consumer.uses(adapterComponent, "Uses");
          }
        }
      }
    }
  }

  /**
   * Search through all the container components for the one matching expected
   * component-class-name.
   */
  protected Optional<Component> findComponentByType(Container container,
      String fullyQualifiedComponentClassName) {
    for (Component component : container.getComponents()) {
      if (fullyQualifiedComponentClassName.equals(
          component.getProperties().get("component.type"))) {
        log.info("C4-docs generator - found component marked by '{}': {}",
            fullyQualifiedComponentClassName, component);
        return Optional.of(component);
      }
    }
    return Optional.empty();
  }

  /**
   * Determines which specific C4ExternalSystem annotation types are actually applied to classes in
   * the codebase (e.g. C4ExternalSystemAdapter, C4ExternalSystemAdapter.RestApi,
   * C4ExternalSystemAdapter.Kafka).
   */
  private Set<String> findUsedC4ExternalSystemAnnotationTypes() {
    Set<String> used = new LinkedHashSet<>();
    for (C4ExternalSystemData data : findC4ExternalSystems().values()) {
      used.add(data.annotationType());
    }
    log.info("C4-docs generator - found {} usages of @C4ExternalSystemAdapter: {}",
        used.size(), used);
    return used;
  }

  // -- ClassGraph-based filesystem scanning (no classpath required for scanned classes) -----------

  /**
   * Scans compiled classes from the filesystem using ClassGraph. Discovers classes annotated with
   * {@link C4ExternalSystemAdapter} (directly or via composed annotations like
   * {@link C4ExternalSystemAdapter.Kafka}) and caches annotation data plus class structure info.
   */
  private void ensureScanned() {
    if (cachedC4ExternalSystems != null) {
      return;
    }

    cachedC4ExternalSystems = new LinkedHashMap<>();
    cachedClassInterfaces = new LinkedHashMap<>();
    cachedClassFieldTypes = new LinkedHashMap<>();

    String generatorClassesPath = C4ExternalSystemAdapter.class
        .getProtectionDomain().getCodeSource().getLocation().getPath();

    log.info(
        "C4-docs generator - filesystem scanning for @C4ExternalSystemAdapter in package '{}' (folder: '{}') - starting",
        basePackageForScanning, pathToJarOrFolderWithCompiledClasses);

    try (ScanResult scanResult = new ClassGraph()
        .overrideClasspath(pathToJarOrFolderWithCompiledClasses, generatorClassesPath)
        .enableAllInfo()
        .scan()) {

      String c4AnnotationName = C4ExternalSystemAdapter.class.getName();

      // Find classes with @C4ExternalSystemAdapter (including via meta-annotations)
      for (ClassInfo ci : scanResult.getClassesWithAnnotation(c4AnnotationName)) {
        if (ci.isAnnotation()) {
          continue;
        }
        if (!ci.getPackageName().startsWith(basePackageForScanning)) {
          continue;
        }

        C4ExternalSystemData data = extractC4AnnotationData(ci, c4AnnotationName);
        if (data != null) {
          cachedC4ExternalSystems.put(ci.getName(), data);
          log.info("C4-docs generator - found @C4ExternalSystemAdapter marked class: {}",
              ci.getName());
        }
      }

      // Cache class structure info for relationship resolution
      for (ClassInfo ci : scanResult.getAllClasses()) {
        if (!ci.getPackageName().startsWith(basePackageForScanning)) {
          continue;
        }
        if (ci.isAnnotation()) {
          continue;
        }

        cachedClassInterfaces.put(ci.getName(), ci.getInterfaces().getNames());

        List<String> fieldTypes = ci.getDeclaredFieldInfo().stream()
            .map(this::getFieldTypeName)
            .filter(Objects::nonNull)
            .toList();
        cachedClassFieldTypes.put(ci.getName(), fieldTypes);
      }
    }

    log.info("C4-docs generator - filesystem scanning completed. Found {} external systems: {}",
        cachedC4ExternalSystems.size(), cachedC4ExternalSystems.keySet());
  }

  /**
   * Extracts {@link C4ExternalSystemData} from a class that has {@link C4ExternalSystemAdapter}
   * either directly or via a composed annotation.
   */
  private C4ExternalSystemData extractC4AnnotationData(ClassInfo classInfo,
      String c4AnnotationName) {
    // Check direct @C4ExternalSystemAdapter first
    AnnotationInfo direct = classInfo.getAnnotationInfo(c4AnnotationName);
    if (direct != null) {
      return toC4Data(classInfo.getName(), direct);
    }

    // Check composed annotations (meta-annotated with @C4ExternalSystemAdapter)
    for (AnnotationInfo ai : classInfo.getAnnotationInfo()) {
      ClassInfo annClassInfo = ai.getClassInfo();
      if (annClassInfo != null && annClassInfo.hasAnnotation(c4AnnotationName)) {
        return toC4Data(classInfo.getName(), ai);
      }
    }

    return null;
  }

  private C4ExternalSystemData toC4Data(String className, AnnotationInfo ai) {
    AnnotationParameterValueList params = ai.getParameterValues();
    return new C4ExternalSystemData(
        className,
        getStringParam(params, "externalSystemName"),
        getStringParam(params, "description"),
        getStringParam(params, "relationship"),
        getStringParam(params, "technology"),
        ai.getName()
    );
  }

  private String getStringParam(AnnotationParameterValueList params, String name) {
    var paramValue = params.get(name);
    return paramValue != null ? String.valueOf(paramValue.getValue()) : "";
  }

  private String getFieldTypeName(FieldInfo fi) {
    var type = fi.getTypeSignatureOrTypeDescriptor();
    if (type instanceof ClassRefTypeSignature classRef) {
      return classRef.getFullyQualifiedClassName();
    }
    return null;
  }

  protected Map<String, C4ExternalSystemData> findC4ExternalSystems() {
    ensureScanned();
    return cachedC4ExternalSystems;
  }

  private List<String> getClassInterfaces(String className) {
    ensureScanned();
    return cachedClassInterfaces.getOrDefault(className, List.of());
  }

  private List<String> getClassFieldTypes(String className) {
    ensureScanned();
    return cachedClassFieldTypes.getOrDefault(className, List.of());
  }

  // -- Grouping, views, export --------------------------------------------------------------------

  protected void groupComponentsForBetterVisualEffect(Container service) {
    for (Component component : service.getComponents()) {
      String tech = component.getTechnology();
      try {
        switch (ComponentType.valueOf(tech)) {
          case INTERFACES_PUBLIC_API:
          case INTERFACES_DOMAIN_API:
          case INTERFACES_WEBHOOK:
          case INTERFACES_MESSAGE_CONSUMER:
          case INTERFACES_CRON_JOB:
          case INTERFACES_STREAM_PROCESSOR:
          case INTERFACES_ASYNC_WORKER:
            component.setGroup("Interfaces");
            break;
          case DOMAIN_SERVICE:
            component.setGroup("Domain");
            break;
          case INFRASTRUCTURE_ADAPTER:
            component.setGroup("Infrastructure");
            break;
        }
      } catch (IllegalArgumentException ignored) {
        log.warn(
            "C4-docs generator - invalid component technology: '{}' set for the component: '{}'",
            tech, component);
      }
    }
  }

  protected void createViews(Workspace workspace, Container service) {
    // --- Views ---
    ViewSet views = workspace.getViews();

    // Level 1: System Context
    SystemContextView contextView = views.createSystemContextView(
        service.getSoftwareSystem(), "SystemContext",
        "System Context diagram.");
    contextView.addAllSoftwareSystems();
    contextView.addAllPeople();

    // Level 2: Container
    ContainerView containerView = views.createContainerView(
        service.getSoftwareSystem(), "Containers",
        "Container diagram.");
    containerView.addAllContainers();
    containerView.addAllSoftwareSystems();
    containerView.addAllPeople();

    // Level 3: Component
    ComponentView componentView = views.createComponentView(
        service, "Components",
        "Component diagram.");
    componentView.addAllComponents();
    componentView.addAllSoftwareSystems();
    componentView.addAllPeople();

    // --- Styling ---
    applyStyling(views);
  }

  protected void applyStyling(ViewSet views) {
    Styles styles = views.getConfiguration().getStyles();
    styles.addElementStyle(Tags.SOFTWARE_SYSTEM)
        .background("#1168bd").color("#ffffff");
    styles.addElementStyle(Tags.PERSON)
        .background("#08427b").color("#ffffff").shape(Shape.Person);
    styles.addElementStyle(Tags.CONTAINER)
        .background("#438dd5").color("#ffffff");
    styles.addElementStyle(Tags.COMPONENT)
        .background("#85bbf0").color("#000000");
  }

  protected void exportToJson(Workspace workspace) throws Exception {
    File outputDir = new File(outputFolder);
    outputDir.mkdirs();
    com.structurizr.util.WorkspaceUtils.saveWorkspaceToJson(
        workspace, new File(outputDir, "workspace.json"));
    log.info("C4-docs generator - generated output - JSON - location: {}", outputFolder);
  }

  protected void exportToPlantUml(Workspace workspace) {
    File outputDir = new File(outputFolder);
    outputDir.mkdirs();

    StructurizrPlantUMLExporter exporter = new StructurizrPlantUMLExporter();
    exporter.export(workspace).forEach(diagram -> {
      File file = new File(outputDir, diagram.getKey() + ".puml");
      try (FileWriter writer = new FileWriter(file)) {
        writer.write(diagram.getDefinition());
      } catch (IOException e) {
        throw new RuntimeException("Failed to write " + file.getName(), e);
      }
    });
    log.info("C4-docs generator - generated output - PlantUML - location: {}", outputFolder);
  }

  /**
   * Component types which will be used to mark components found during auto-discovery by class
   * scanning.
   */
  public enum ComponentType {
    //primary ports
    INTERFACES_PUBLIC_API,
    INTERFACES_DOMAIN_API,
    INTERFACES_WEBHOOK,
    INTERFACES_MESSAGE_CONSUMER,
    INTERFACES_CRON_JOB,
    INTERFACES_STREAM_PROCESSOR,
    INTERFACES_ASYNC_WORKER,
    //business domain,
    DOMAIN_SERVICE,
    //secondary ports
    INFRASTRUCTURE_ADAPTER,
  }
}

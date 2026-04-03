package no.catenda.docs.c4;

import com.structurizr.component.naming.FullyQualifiedNamingStrategy;
import com.structurizr.component.naming.NamingStrategy;
import com.structurizr.component.naming.TypeNamingStrategy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import no.catenda.docs.c4.C4DiagramGeneratorParent.ComponentType;
import no.catenda.docs.c4.dropwizard.C4DiagramGeneratorBimsync;
import no.catenda.docs.c4.springboot.C4DiagramGeneratorSpringBoot;

@UtilityClass
@Slf4j
public class C4DiagramGeneratorStarter {

  public static final String DEFAULT_OUTPUT_FOLDER = "target/docs-c4";

  public static final String DEFAULT_BASE_PACKAGE = "no.catenda";

  public static final Supplier<NamingStrategy> DEFAULT_COMPONENT_NAMING_STRATEGY = () -> new TypeNamingStrategy();

  /**
   * Folder which should be scanned for jar / compiled classes
   */
  public static final String ENV_FOLDER_TO_SCAN = "folderToScan";
  /**
   * Java packages which should be scanned to skip some of the classes
   */
  public static final String ENV_PACKAGE_TO_SCAN = "packageToScan";
  /**
   * Output
   */
  public static final String ENV_OUTPUT_FOLDER = "outputFolder";
  /**
   * Comma-separated list of annotation=ComponentType pairs, e.g.
   * "org.springframework.web.bind.annotation.RestController=INTERFACES_PUBLIC_API,org.springframework.stereotype.Service=DOMAIN_SERVICE"
   */
  public static final String ENV_COMPONENT_FINDING_STRATEGIES = "componentFindingStrategies";

  public static String getEnvOrDefault(String envVar, String defaultValue) {
    String value = System.getenv(envVar);
    if (value != null && !value.isBlank()) {
      log.info("C4-docs - starting with '{}'='{}' (optional - provided by environment variables)",
          envVar, value);
      return value;
    } else {
      log.info("C4-docs - starting with '{}'='{}' (optional - using default as empty provided)",
          envVar, defaultValue);
      return defaultValue;
    }
  }

  public static Map<String, ComponentType> getStrategiesFromEnv() {
    var raw = getEnvOrException(ENV_COMPONENT_FINDING_STRATEGIES);
    var result = new HashMap<String, ComponentType>();
    for (String entry : raw.split(",")) {
      String[] parts = entry.split("=", 2);
      if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
        throw new IllegalArgumentException(
            "Invalid strategy format: '" + entry
                + "'. Expected: fullyQualifiedAnnotation=ComponentType");
      }
      String annotation = parts[0].trim();
      ComponentType type = ComponentType.valueOf(parts[1].trim());
      log.info("C4-docs - adding component strategy: '{}' -> '{}'", annotation, type);
      result.put(annotation, type);
    }
    return result;
  }

  public static String getEnvOrException(String envVar) {
    String value = System.getenv(envVar);
    if (value == null || value.isBlank()) {
      log.info("C4-docs - environment variable not provided: '{}'", envVar);
      throw new IllegalArgumentException("Mandatory environment variable not present: " + envVar);
    }
    log.info("C4-docs - starting with '{}'='{}' (mandatory - provided by environment variables)",
        envVar, value);
    return value;
  }

  @SneakyThrows
  public static void startSpringBoot(String systemName, String containerName) {
    var folderWithCompiledClasses = getEnvOrException(ENV_FOLDER_TO_SCAN);
    var basePackageForScanning = getEnvOrDefault(ENV_PACKAGE_TO_SCAN, DEFAULT_BASE_PACKAGE);
    var outputFolder = getEnvOrDefault(ENV_OUTPUT_FOLDER, DEFAULT_OUTPUT_FOLDER);
    var configuration = ComponentFindingConfiguration.builder()
        .registerDefaultC4ExternalSystemAdapterAnnotationFinder(true)
        .namingStrategy(DEFAULT_COMPONENT_NAMING_STRATEGY.get())
        .annotationToComponentType(getStrategiesFromEnv())
        .build();

    new C4DiagramGeneratorSpringBoot(folderWithCompiledClasses, basePackageForScanning,
        outputFolder, configuration)
        .generateC4Diagram(systemName, containerName);
  }

  @SneakyThrows
  public static void startDropwizard(String systemName, String containerName) {
    var folderWithCompiledClasses = getEnvOrException(ENV_FOLDER_TO_SCAN);
    var basePackageForScanning = getEnvOrDefault(ENV_PACKAGE_TO_SCAN, DEFAULT_BASE_PACKAGE);
    var outputFolder = getEnvOrDefault(ENV_OUTPUT_FOLDER, DEFAULT_OUTPUT_FOLDER);
    var configuration = ComponentFindingConfiguration.builder()
        .registerDefaultC4ExternalSystemAdapterAnnotationFinder(true)
        .namingStrategy(new FullyQualifiedNamingStrategy())
        .annotationToComponentType(getStrategiesFromEnv())
        .build();

    new C4DiagramGeneratorBimsync(folderWithCompiledClasses, basePackageForScanning, outputFolder,
        configuration)
        .generateC4Diagram(systemName, containerName);
  }
}

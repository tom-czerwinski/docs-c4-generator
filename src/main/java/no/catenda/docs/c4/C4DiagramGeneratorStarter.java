package no.catenda.docs.c4;

import com.structurizr.component.naming.FullyQualifiedNamingStrategy;
import com.structurizr.component.naming.NamingStrategy;
import com.structurizr.component.naming.TypeNamingStrategy;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import no.catenda.docs.c4.C4DiagramGeneratorParent.ComponentType;
import no.catenda.docs.c4.dropwizard.C4DiagramGeneratorBimsync;
import no.catenda.docs.c4.springboot.C4DiagramGeneratorSpringBoot;

@UtilityClass
public class C4DiagramGeneratorStarter {

  public static final String DEFAULT_OUTPUT_FOLDER = "target/docs-c4";

  public static final String DEFAULT_CLASSES_DIR = "target/classes";

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

  public static String getEnvOrDefault(String envVar, String defaultValue) {
    String value = System.getenv(envVar);
    return value != null && !value.isBlank() ? value : defaultValue;
  }

  public static String getEnvOrException(String envVar) {
    String value = System.getenv(envVar);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Mandatory environment variable not present: " + envVar);
    }
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
        .build()
        .addStrategy("org.springframework.web.bind.annotation.RestController",
            ComponentType.INTERFACES_PUBLIC_API)
        .addStrategy("org.springframework.stereotype.Service", ComponentType.DOMAIN_SERVICE);

    new C4DiagramGeneratorSpringBoot(folderWithCompiledClasses, basePackageForScanning,
        outputFolder, configuration)
        .generateC4Diagram(systemName, containerName);
  }

  @SneakyThrows
  public static void startDropwizart(String systemName, String containerName) {
    var folderWithCompiledClasses = getEnvOrException(ENV_FOLDER_TO_SCAN);
    var basePackageForScanning = getEnvOrDefault(ENV_PACKAGE_TO_SCAN, DEFAULT_BASE_PACKAGE);
    var outputFolder = getEnvOrDefault(ENV_OUTPUT_FOLDER, DEFAULT_OUTPUT_FOLDER);
    var configuration = ComponentFindingConfiguration.builder()
        .registerDefaultC4ExternalSystemAdapterAnnotationFinder(true)
        .namingStrategy(new FullyQualifiedNamingStrategy())
        .build()
        .addStrategy("javax.ws.rs.Path", ComponentType.INTERFACES_PUBLIC_API);

    new C4DiagramGeneratorBimsync(folderWithCompiledClasses, basePackageForScanning, outputFolder,
        configuration)
        .generateC4Diagram(systemName, containerName);
  }
}

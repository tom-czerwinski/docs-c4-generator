package no.catenda.docs.c4;

import com.structurizr.component.naming.NamingStrategy;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import no.catenda.docs.c4.C4DiagramGeneratorParent.ComponentType;

@ToString
@Builder(access = AccessLevel.PUBLIC)
public class ComponentFindingConfiguration {

  private final Map<String, ComponentType> annotationToComponentType = new HashMap<>();

  @Getter
  private final NamingStrategy namingStrategy;

  @Getter
  private final boolean registerDefaultC4ExternalSystemAdapterAnnotationFinder;

  public ComponentFindingConfiguration addStrategy(String fullyQualifiedAnnotationClassName,
      ComponentType componentType) {
    annotationToComponentType.put(fullyQualifiedAnnotationClassName, componentType);
    return this;
  }

  public Stream<Entry<String, ComponentType>> streamStrategies() {
    return annotationToComponentType.entrySet().stream();
  }

}

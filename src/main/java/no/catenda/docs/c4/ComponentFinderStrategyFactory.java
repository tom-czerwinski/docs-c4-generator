package no.catenda.docs.c4;

import com.structurizr.component.ComponentFinderStrategy;
import com.structurizr.component.ComponentFinderStrategyBuilder;
import com.structurizr.component.matcher.AnnotationTypeMatcher;
import com.structurizr.component.supporting.DefaultSupportingTypesStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.catenda.docs.c4.C4DiagramGeneratorParent.ComponentType;
import java.util.stream.Stream;

@Slf4j
class ComponentFinderStrategyFactory {

  @Getter
  private final ComponentFindingConfiguration configuration;

  ComponentFinderStrategyFactory(ComponentFindingConfiguration configuration) {
    this.configuration = configuration;
    log.info("Using component-finder-strategy configuration: {}", configuration);
  }

  Stream<ComponentFinderStrategy> createConfiguredComponentFinders(){
    return configuration.streamStrategies()
        .map(strategy -> createComponentFinder(strategy.getKey(), strategy.getValue()));
  }


  ComponentFinderStrategy createComponentFinder(String annotationType,
      ComponentType infrastructureAdapter) {
    return new ComponentFinderStrategyBuilder()
        .matchedBy(new AnnotationTypeMatcher(
            annotationType))
        .withTechnology(infrastructureAdapter.name())
        .withName(configuration.getNamingStrategy())
        //supporting strategy applies relations by default
        //AllReferencedTypesSupportingTypesStrategy - least restrictive
        //DefaultSupportingTypesStrategy - most restrictive, only when real access
        .supportedBy(new DefaultSupportingTypesStrategy())
        .build();
  }


}

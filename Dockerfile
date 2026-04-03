FROM amazoncorretto:25
WORKDIR /app
COPY target/c4-generator-1.0.0-SNAPSHOT.jar /app/c4-generator.jar

ENV GENERATOR_CLASS=no.catenda.docs.c4.springboot.C4DiagramGeneratorSpringBoot
ENV folderToScan=/input
ENV outputFolder=/output
ENV packageToScan=no.catenda

ENTRYPOINT ["sh", "-c", "java -cp /app/c4-generator.jar ${GENERATOR_CLASS}"]

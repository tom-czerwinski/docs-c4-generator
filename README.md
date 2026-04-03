# C4 Documentation generator

Allows to generate C4 models from targeted Java-compiled-code.

## Running the generator

The generator has currently two entry points depending on the framework used by the target service:

- **Spring Boot** — `no.catenda.docs.c4.springboot.C4DiagramGeneratorSpringBoot`
- **Dropwizard** — `no.catenda.docs.c4.dropwizard.C4DiagramGeneratorBimsync`

Run the desired `main` class with the required environment variables set (see below).

### Environment variables

| Variable        | Required | Default            | Description                                                   |
|-----------------|----------|--------------------|---------------------------------------------------------------|
| `folderToScan`  | **Yes**  | —                  | Path to the folder (or JAR) containing compiled classes to scan |
| `packageToScan` | No       | `no.catenda`       | Base Java package used to filter scanned classes               |
| `outputFolder`  | No       | `target/docs-c4`   | Directory where generated diagrams are written                 |

### IntelliJ IDEA run configurations

Two shared run configurations are included in `.idea/runConfigurations/`:

| Configuration                        | Main class                             | `folderToScan` default                                                     |
|--------------------------------------|----------------------------------------|----------------------------------------------------------------------------|
| **generate C4 - local SpringBoot**   | `...springboot.C4DiagramGeneratorSpringBoot` | `$USER_HOME$/tk-tests/test-component86/test-component-service/target/classes` |
| **generate C4 - Bimsync Arena Edge** | `...dropwizard.C4DiagramGeneratorBimsync`    | `$PROJECT_DIR$/../bimsync/bimsync-arena-edge/target`                        |

Open the project in IntelliJ IDEA and the configurations will appear in the **Run/Debug** dropdown. Adjust the `folderToScan` value to point at the compiled classes of the service you want to document.

## Viewing generated diagrams (Docker Compose)

A `docker-compose.yaml` is provided to start [Structurizr Lite](https://structurizr.com/), a local viewer for the generated `workspace.json`.

Start the viewer:

```bash
docker-compose up -d
```

The UI is available at **http://localhost:8180**. It mounts `./target/docs-c4` so any regenerated diagrams are picked up automatically (refresh the browser).

Stop and remove the container:

```bash
docker-compose down
```


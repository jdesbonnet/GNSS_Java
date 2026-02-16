# Getting Started

## Prerequisites

- Java 11+
- Maven 3.8+

## Common commands

Build and test:

```bash
mvn clean verify
```

Generate JavaDoc only:

```bash
mvn javadoc:javadoc
```

## Local docs preview

Install docs dependencies:

```bash
pip install mkdocs mkdocs-material
```

Run local docs server:

```bash
mkdocs serve
```

Then open `http://127.0.0.1:8000`.

## API docs

Generated JavaDoc is available from the [API Reference](api.md) page.

# GNSS Java

GNSS Java is a Maven-based Java project for working with GNSS/NMEA data streams, including tools and supporting resources for parsing, testing, and visualizing satellite data.

## Documentation

A documentation site is set up with MkDocs and published via GitHub Pages, including generated JavaDoc.

- Docs source lives in [`docs/`](docs/)
- Site config is in [`mkdocs.yml`](mkdocs.yml)
- GitHub Actions workflow is in [`.github/workflows/docs.yml`](.github/workflows/docs.yml)

### Build docs locally

Generate JavaDoc:

```bash
mvn javadoc:javadoc
```

Build the Markdown docs site:

```bash
pip install mkdocs mkdocs-material
mkdocs build
```

Then copy JavaDoc into the built site output:

```bash
mkdir -p site/apidocs
cp -R target/site/apidocs/* site/apidocs/
```

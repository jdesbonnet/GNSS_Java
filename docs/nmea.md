# NMEA Guide

This project includes support for parsing and handling common NMEA sentence types.

## Typical flow

1. Read incoming NMEA sentence text lines.
2. Use parser utilities/factories to parse into sentence-specific models.
3. Process typed sentence objects for downstream GNSS logic.

## Tips

- Validate sentence checksums where appropriate.
- Keep stream parsing resilient to malformed lines.
- Favor typed sentence classes over string slicing in application logic.

For class-level details, consult the generated JavaDoc.

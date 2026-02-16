# NTRIP & RTCM

GNSS Java also includes components for NTRIP workflows and RTCM parsing.

## NTRIP

- Client functionality for connecting to casters and consuming correction data.
- Caster-side support components for station and rover interactions.

## RTCM

- Parser support for RTCM v3 message handling.
- Integration points to process correction streams in real time.

## Operational notes

- Keep connection credentials and endpoints in environment-specific config.
- Monitor stream quality and reconnect behavior for long-running sessions.
- Use logging around connection lifecycle and message parsing.

For API-level usage and class details, see JavaDoc.

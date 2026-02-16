# GNSS_Java Review: Suggested Engineering Tasks

This backlog focuses on making non-GGA NMEA support robust and production-ready.

## P0 — Correctness bugs to fix first

1. **Fix GSV parsing bugs and resilience**
   - `nSat` is hardcoded (`Integer.valueOf(3)`) instead of reading field 3.
   - `messageNumber` is defined but never populated.
   - Empty PRN/elevation/azimuth/SNR fields currently throw parse errors in weak-signal situations.
   - Validate handling of optional NMEA 4.10 signal/system-id tail field.

2. **Complete and correct RMC field parsing**
   - `magvarStr` is read from `parts[9]` (same as date), so indices are currently incorrect.
   - Speed/course/status are extracted but never persisted to class fields.
   - Add handling for status (`A`/`V`) and empty date/time fields.

3. **Remove latent timestamp parsing edge cases**
   - `PVT.fromGGA()` assumes decimal fractions are always present (`substring(7)`), which can fail on valid `hhmmss` formats.
   - Unify NMEA time parsing across `Util`, `GGA`, and `PVT` to support 0/1/2/3 fractional digits consistently.

## P1 — Expand sentence support beyond GGA/GSV/RMC

4. **Introduce additional core sentence parsers**
   - Add classes and parsing for at least: `GSA`, `GST`, `VTG`, and `GLL`.
   - These cover DOP/active satellites, pseudorange error estimates, track/speed, and position redundancy.

5. **Refactor sentence dispatch into an extensible registry**
   - `Sentence.valueOf()` currently hardcodes a switch and returns `null` for unknown types.
   - Replace with a registry/factory map to simplify adding sentence implementations and enable graceful unknown-sentence handling.

6. **Define normalized epoch model in `Stream`**
   - Extend epoch aggregation beyond GGA-only data:
     - from `RMC`: date, speed over ground, track angle
     - from `GSA`: PDOP/HDOP/VDOP + fix mode
     - from `GST`: position uncertainty estimates
   - Emit one coherent per-epoch object with provenance.

## P2 — Performance and architecture cleanup

7. **Finish or remove unfinished fast parser path**
   - `ParserFactory.createParserByExample()` returns `null`.
   - `Parser.parseGga()` is effectively a stub.
   - Decide to either complete the zero-allocation parser design or deprecate these classes to avoid dead API surface.

8. **Introduce structured parse errors instead of null-driven flow**
   - Current flow often logs and returns `null`.
   - Use typed parse outcomes (success / checksum-fail / unsupported / malformed) for better telemetry and debugging.

9. **Improve constellation/talker handling**
   - Confirm handling of mixed-talker streams (`GN`, `GP`, `GL`, `GA`, `GB`) and proprietary talkers.
   - Add fallback behavior for unknown talkers instead of silent `null` constellation values.

## P3 — Test strategy upgrades

10. **Strengthen sentence test coverage and assertions**
   - `testGSV()` currently has no `@Test` annotation and performs no assertions.
   - Add deterministic assertions for parsed fields, optional fields, and malformed inputs.

11. **Add conformance fixtures per sentence type and NMEA version**
   - Include fixtures for NMEA 4.10+ variants with signal IDs and missing-field edge cases.
   - Build parameterized tests across constellations/talkers.

12. **Add round-trip and stream-level behavior tests**
   - Validate sentence-by-sentence parsing and epoch assembly from mixed real logs.
   - Include day rollover and date synchronization scenarios (`RMC` + `GGA`).

## Suggested implementation order (first 2 sprints)

- **Sprint 1:** Items 1, 2, 3, 10.
- **Sprint 2:** Items 4, 5, 6, 11.
- **Sprint 3+:** Items 7, 8, 9, 12.

# Stress test — 2026-09-23

Emulator (Pixel-class, API 37). Data injected into the app database:
47 active reasons (long names, one 70-char word without spaces, Czech diacritics, emoji, Arabic, Japanese, Cyrillic),
4,994 events (30 days × ~150 + ~500 today), ~200 distinct typed answers (one 400 chars), 2 events in the future.

## Measurements

| What | Result |
|---|---|
| Sorting reasons for the question (47 reasons, 5,000 events) | ~19 ms (was ~1 ms with small data) |
| Unlock → question drawn | 88–148 ms |
| Memory (PSS) | ~100 MB |
| Jar, ~500 pebbles, while moving | ~9 fps (target 60); 65 % janky frames |
| Jar at rest, ~500 pebbles | **never rests** (still ~9 fps after 28 s) |
| Jar at rest, ~40 pebbles | 0 frames (rests) |
| Today screen, cold start with 5,000 events | empty for >4 s before data shows (cause not measured) |
| Crashes | none |

## Findings

| # | Severity | Finding | Cause |
|---|---|---|---|
| 1 | High | Jar with many pebbles is chaotic, never rests, ~9 fps, drains battery | Collision check is all-pairs (~1 M checks/frame at 500); rest rule needs every pebble slow; deep piles never get there |
| 2 | High | After a size change (rotation, font scale) all pebbles restart from the layout on top of each other and explode across the jar | World is recreated when constraints change |
| 3 | Medium | Pebbles cover the unlock number from ~400 unlocks | Minimum pebble size 14 dp, pile allowed to fill the jar |
| 4 | Medium | Long reason names are cut mid-line in the question chips and in the Reasons list | Fixed heights, no ellipsis |
| 5 | Medium | Font size 2×: Today number breaks into "4 / 9 / 8"; "Just habit" subtitle cut | Badge shares the row with the number; fixed button height |
| 6 | Medium | Today legend lists every reason (40+ chips) | No limit |
| 7 | Low | Sorting 19 ms with 5,000 events | Converting every timestamp to a zoned date |
| 8 | Low | Today empty for >4 s on cold start with big data | Not measured yet |
| 9 | Low | Question window redraws continuously while open (spinning habit star) | Infinite animation; costs battery only while the question is shown |

## Worked well

- 15 lock/unlock cycles within 15 s: no duplicate sessions, no crash.
- Question with 47 reasons: grid scrolls, fade hint, habit/field/pause stay pinned.
- All reasons archived: question shows only "Just habit" and the text field.
- Events in the future: no crash; counted, duration 0.
- 145-char typed answer: stored and shown (adb cut the 500-char input).
- Arabic, Japanese, emoji and diacritics render correctly.

## Not tested

- Real clock change (needs root on the emulator; simulated with future events).
- Landscape: the rotation setting did not rotate the emulator in this run.
- Real phone performance (the phone was not connected).

## After fixes (same data, same emulator)

| What | Before | After |
|---|---|---|
| Jar physics per frame, ~500 pebbles (debug build) | 13–18 ms | ~6 ms |
| Jar drawing per frame (debug build) | 8–9 ms | ~0.4 ms |
| Jar janky frames while tilting/shaking | 63–65 % | 0.5 % (debug), 4 % (release, incl. shaking) |
| Jar at rest | never | 0 frames (quiet, or at most 3 s after the last tilt change) |
| Sorting for the question (debug build) | 19–35 ms | 19–23 ms (grouping 18 → 4.5 ms, ranking 6 → 4.7 ms; the rest is reading 2,881 rows) |
| Memory, release build | — | ~33 MB PSS |

Changes: neighbour grid with pairs built once per substep, rest limit of 3 s, 3 solver passes, pebbles
drawn as pre-rendered bitmaps (uploaded before the first frame), world keeps pebbles on resize,
minimum pebble 10 dp, text limited to 2 lines with ellipsis, heights grow with font size,
badge wraps under the number only when there is no room, legend shows 6 + "more",
habit star turns once instead of forever, capital-letter labels shown calmly,
Material transitions (fade through for tabs, shared axis X for Settings).

Release build (R8) smoke test: unlock → question → answer, Settings, all tabs — no crash.
Not checked in release: the saved answer in the database (`run-as` needs a debuggable build).

## Jar on dyn4j (same data, release build, emulator)

| What | Result |
|---|---|
| 511 pebbles, tilting for 12 s | 736 frames, 0.14 % janky, 90th percentile 18 ms |
| Physics per frame, 511 pebbles (host JVM) | 3.1 ms median, 3.4 ms p90 |
| Pebbles out of the jar after hard shaking | 0 |
| Jar at rest | 0 frames, 3.5 s after the phone stops moving |
| Today cold start, same data (1,563 events in the last week) | data shown at 0.66 s after process start; earlier >4 s was with the old jar busy on the main thread (~110 ms per frame) |

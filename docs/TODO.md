# TODO

## Next
- [x] Reasons sorted by time of day: ±90 min window over the last 30 days, weekdays and weekends apart, fallback to all days, then 30-day use. "Just habit" always last.
- [x] Remove manual reason order (Move up/down); Reasons screen sorts by use.
- [x] Earlier typed answers as chips when the "Something else" field is focused; filter while typing.
- [x] Group typed answers ignoring case, spaces, end punctuation and diacritics.
- [x] Offer a typed answer as a reason at 5, 10, 15… entries, in the question window: Add / Not now / Don't ask again. Add moves earlier entries to the new reason.
- [x] Many reasons: reason grid scrolls with a bottom fade; "Just habit", "Something else" and "Pause" stay pinned.
- [x] Measure prompt build time after the sorting change: 3.4 ms cold, ~1 ms warm (emulator). Check on the phone too.

## Afterwards
- [x] Evening reflection: one quiet notification with the day's numbers, default on at 21:00. Settings: on/off and time (20–23 h).
- [x] Habit hours: weekday × hour heat map of habit unlocks on the Week screen, last 4 weeks, with the peak hour.

## Ideas, not decided
- [ ] Smart nudge timing per reason (from its typical session length).
- [ ] Breathing pause after repeated habit unlocks.
- [ ] Quiet hours / bedtime mode.
- [ ] Kind daily goal with a forgiving streak.
- [ ] Export my data (CSV through the system file picker).
- [x] Health check: after the system stops the app, restart the service on the next process start; if Android blocks it, a "Questions are off" notification restarts it with one tap.
- [ ] One-tap battery exemption dialog (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS). Play allows it only when Doze breaks the core function; decide before a store release.
- [ ] Jar fallback above ~390 pebbles a day.

- [ ] After a reboot the service starts only when Android delivers BOOT_COMPLETED (can take a minute on Motorola); unlocks before that are missed. Measure on the phone, then consider direct boot or an earlier signal.
- [ ] Rare: an unlock stored without a visible question (seen ~3× in 40 automated unlocks, never reproduced directly).

## Before release
- [x] Donation link: https://ko-fi.com/tukak (`SettingsScreen.kt`, README).
- [ ] Release signing.
- [ ] Translations (Czech first).

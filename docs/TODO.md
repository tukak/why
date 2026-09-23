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
- [ ] Health check: hint when unlocks were missed because the service was stopped.
- [ ] Jar fallback above ~390 pebbles a day.

## Before release
- [ ] Real "Buy me a coffee" URL (`SettingsScreen.kt`).
- [ ] Release signing.
- [ ] Translations (Czech first).

# Extra R8 rules for the `benchmark` build type only.
#
# The benchmark type keeps release's optimisation so timings stay realistic, but
# obfuscated names turn a trace into a wall of a(), b(), c(). Keeping the names
# costs nothing at runtime and makes a regression readable.
-dontobfuscate

# Macrobenchmark drives the app from a separate process via UiAutomator, so the
# entry points it touches must survive shrinking.
-keep class com.fairtrack.app.MainActivity { *; }

# power-server

This project is meant to provide a REST endpoint streaming power consumption, inspired
by [JoularJX](https://github.com/joular/joularjx) but focusing on exposing power information and metadata over REST
without further processing.

Only Linux/amd64 and macOS (amd64/apple silicon) are supported at the moment. Of note, this tool needs to be run
via `sudo` because power consumption information is considered as security sensitive (as it can
enable [side-channel attacks](https://en.wikipedia.org/wiki/Side-channel_attack)) See below for platform-specific
information.

### macOS

Power monitoring is performed using the
bundled `[powermetrics](https://developer.apple.com/library/archive/documentation/Performance/Conceptual/power_efficiency_guidelines_osx/PrioritizeWorkAtTheTaskLevel.html#//apple_ref/doc/uid/TP40013929-CH35-SW10)`
tool, which is run with specific parameters and which
output is then parsed into a usable representation.

### Linux

Power monitoring leverages Intel's RAPL technology via
the `[powercap](https://www.kernel.org/doc/html/latest/power/powercap/powercap.html)` framework.
In particular, this requires read access to files located in the `/sys/class/powercap/intel-rapl` directory. For
security purposes, some of that information is only readable by root, in particular the values that we need to get the
power consumption, currently:

- `/sys/class/powercap/intel-rapl/intel-rapl:1/energy_uj` (if available)
- `/sys/class/powercap/intel-rapl/intel-rapl:0/energy_uj`
- `/sys/class/powercap/intel-rapl/intel-rapl:0/intel-rapl:0:2/energy_uj`

These files are read periodically and the read energy value is then used to compute power information based on the
sampling frequency.

This project uses [Quarkus](https://quarkus.io), the Supersonic Subatomic Java Framework.
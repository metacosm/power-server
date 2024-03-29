# power-server

This project is meant to provide a REST endpoint streaming power consumption, inspired
by [JoularJX](https://github.com/joular/joularjx) but focusing on exposing power information and metadata over REST
without further processing.

This project uses [Quarkus](https://quarkus.io), the Supersonic Subatomic Java Framework and comprises 3 modules:

- `build-tools`, which contains formatting configuration that you can use to set your IDE up to contribute to this
  project
- `metadata`, which contains the metadata API that the RESTful server uses to provide information about what is returned
  by the power sensors. This artifact contains classes that can be reused in client projects.
- `server` contains the RESTful server, listening by default on port `20432` (as specified
  in `[application.properties](https://github.com/metacosm/power-server/blob/87bba3196fa0e552665b4f1d22006377779b0959/server/src/main/resources/application.properties#L1)`)

The server provides two endpoints: `/power/metadata` and `/power/{pid}` where `pid` is a String representation of a
process identifier, identifying a process running on the machine where `power-server` is running.
The metadata endpoint provides information about how measures streamed from the main endpoint is formatted as well as
information about power components. The main endpoint streams `SensorMeasure` objects as defined in the `metadata`
module as an array of double measures. Typically, main sensor components are measured in milli Watts for easier
consumption but clients should check the information provided by the metadata endpoint to learn the layout of the
measures array and which meaning they carry. For example, the macOS implementation provides a decimal percentage measure
of the CPU share of the measured process as part of the returned measure. This information doesn't exist on the Linux
information where the measures are actually energy counters for the whole system, the client being then in charge of
computing the process attribution.

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

## Building and running

### Building

Simply run `mvn clean install` at the root of the project. If you use
the [Quarkus CLI](https://quarkus.io/guides/cli-tooling), you can build using `quarkus build`.

Once you've performed a full build, you can also compile the server to a native binary:

- `cd server`
- `mvn package -Dnative` or `quarkus build --native`

### Running the server

Once build, you can run the server as follows:

- JVM mode: `java -jar server/target/quarkus-app/quarkus-run.jar` from the project root
- Dev mode: `cd server; mvn quarkus:dev` or `cd server; quarkus dev`
- Native mode: `cd server; ./target/*-runner`
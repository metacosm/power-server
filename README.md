# power-server

This project is meant to provide abstractions over power measurement details, which are platform specific, to enable simplified measure of power consumption, inspired by [JoularJX](https://github.com/joular/joularjx) but focusing on exposing power information and metadata. Several tools are provided, included a REST endpoint and a command-line interface.

This project uses [Quarkus](https://quarkus.io), the Supersonic Subatomic Java Framework and comprises several modules:

- `analysis` contains utilities helpful to analyze power measures (computations, statistics, histograms, etc…)
- `backend` provides the core functionality of the power measurement, including the sampling and sensor logic
-  `cli` provides a command-line interface to measure power consumption of a specified command
- `if-manifest-export` provides a means to export a stopped measure as
  a [Green Software Foundation](https://greensoftware.foundation/) [Impact Framework](https://if.greensoftware.foundation/)
  manifest
- `measure` provides classes to help record and process measures in client applications
- `metadata` defines the metadata API that the RESTful server uses to provide information about what is returned
  by the power sensors. This artifact contains classes that can be reused in client projects.
- `persistence` provides support to persist measure data to databases (currently, only to SQLite)
- `server` contains the RESTful server, listening by default on port `20432` (as specified
  in
  `[application.properties](https://github.com/metacosm/power-server/blob/87bba3196fa0e552665b4f1d22006377779b0959/server/src/main/resources/application.properties#L1)`)


The main endpoint streams `SensorMeasure` objects as defined in the `metadata` module as an array of double measures.
Typically, main sensor components are measured in milli Watts for easier consumption but clients should check the
information provided by the metadata endpoint to learn the layout of the measures array and which meaning they carry.
For example, the macOS implementation provides a decimal percentage
measure of the CPU share of the measured process as part of the returned measure. This information doesn't exist on the
Linux implementation where the measures are actually energy counters for the whole system, the client being then in
charge of computing the process attribution.

Only Linux/amd64 and macOS (amd64/apple silicon) are supported at the moment. Of note, this tool needs to be run
privileged code either via running it with `sudo` (preferred) or giving it access to the read the resources it needs
because power consumption information is considered as security sensitive (as it can
enable [side-channel attacks](https://en.wikipedia.org/wiki/Side-channel_attack)) See below for platform-specific
information.

### macOS

Power monitoring is performed using the
bundled
`[powermetrics](https://developer.apple.com/library/archive/documentation/Performance/Conceptual/power_efficiency_guidelines_osx/PrioritizeWorkAtTheTaskLevel.html#//apple_ref/doc/uid/TP40013929-CH35-SW10)`
tool, which is run with specific parameters and which output is then parsed into a usable representation.
There are several options to give access to the tool:

- Add the user running the server to the list of `sudoers` with no-password access to `/usr/bin/powermetrics`
- Run with `sudo` (though this is impractical during development)
- Provide a secret to be able to run the `powermetrics` process (and only this process) using `sudo`. The server will
  look for the secret under the `power-server.sudo.secret` property key. Please look
  at https://quarkus.io/guides/config-secrets for more details on how to do this securely with Quarkus. Note that this
  option is only provided to facilitate development, notably dev mode, and will only work when re-building the project
  with the appropriate dependencies.

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

- JVM mode: `sudo java -jar server/target/quarkus-app/quarkus-run.jar` from the project root
- Dev mode: `cd server; sudo mvn quarkus:dev` or `cd server; sudo quarkus dev` (will only work if `root` has Java
  properly configured)
- Native mode: `sudo ./server/target/*-runner`

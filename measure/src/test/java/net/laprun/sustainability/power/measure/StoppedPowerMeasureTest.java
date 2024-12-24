package net.laprun.sustainability.power.measure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.ComponentProcessor;
import net.laprun.sustainability.power.analysis.MeasureProcessor;
import net.laprun.sustainability.power.analysis.Processors;
import net.laprun.sustainability.power.analysis.total.TotalMeasureProcessor;

public class StoppedPowerMeasureTest {
    private final static SensorMetadata metadata = SensorMetadata
            .withNewComponent("cp1", null, true, "mW")
            .withNewComponent("cp2", null, true, "mW")
            .withNewComponent("cp3", null, true, "mW")
            .build();

    @Test
    void shouldUseInitialProcessorsIfAvailable() {
        final var measure = new OngoingPowerMeasure(metadata);
        final var processor = new TotalMeasureProcessor(metadata, SensorUnit.mW, 0);
        measure.registerMeasureProcessor(processor);
        final var compProc = new ComponentProcessor() {
        };
        measure.registerProcessorFor(0, compProc);

        final var stopped = new StoppedPowerMeasure(measure);
        final var stoppedProcs = stopped.processors();
        assertThat(stoppedProcs).isNotEqualTo(Processors.empty);
        assertThat(stoppedProcs.measureProcessors()).hasSize(1);
        assertThat(stoppedProcs.measureProcessors().getFirst()).isEqualTo(processor);
        assertThat(stoppedProcs.processorsFor(0)).hasSize(1);
        assertThat(stoppedProcs.processorsFor(0).getFirst()).isEqualTo(compProc);
    }

    @Test
    void shouldAlsoUseOwnProcessorsIfAvailable() {
        final var measure = new OngoingPowerMeasure(metadata);
        final var processor = new TotalMeasureProcessor(metadata, SensorUnit.mW, 0);
        measure.registerMeasureProcessor(processor);
        final var ongoingCompProcOutput = "component from ongoing";
        final var compProc = new ComponentProcessor() {
            @Override
            public String output() {
                return ongoingCompProcOutput;
            }
        };
        measure.registerProcessorFor(0, compProc);

        final var stopped = new StoppedPowerMeasure(measure);
        final var stoppedCompProcName = "stopped component processor";
        final var stoppedCompProcOutput = "component from stopped";
        final var stoppedProc = new ComponentProcessor() {
            @Override
            public String name() {
                return stoppedCompProcName;
            }

            @Override
            public String output() {
                return stoppedCompProcOutput;
            }
        };
        stopped.registerProcessorFor(0, stoppedProc);
        final var stoppedMeasureOutput = "measure from stopped";
        final var stoppedMeasureProcName = "stopped measure processor";
        final var stoppedMeasureProc = new MeasureProcessor() {
            @Override
            public String name() {
                return stoppedMeasureProcName;
            }

            @Override
            public String output() {
                return stoppedMeasureOutput;
            }
        };
        stopped.registerMeasureProcessor(stoppedMeasureProc);
        final var stoppedProcs = stopped.processors();
        assertThat(stoppedProcs).isNotEqualTo(Processors.empty);
        final var measureProcessors = stoppedProcs.measureProcessors();
        assertThat(measureProcessors).hasSize(2);
        assertThat(measureProcessors.getFirst()).isEqualTo(processor);
        assertThat(measureProcessors.getLast()).isEqualTo(stoppedMeasureProc);
        final var componentProcessors = stoppedProcs.processorsFor(0);
        assertThat(componentProcessors).hasSize(2);
        assertThat(componentProcessors.getFirst()).isEqualTo(compProc);
        assertThat(componentProcessors.getLast()).isEqualTo(stoppedProc);

        final var output = PowerMeasure.asString(stopped);
        assertThat(output).contains(stoppedCompProcOutput, stoppedMeasureOutput, ongoingCompProcOutput);
        // second anonymous class
        assertThat(output).contains(stoppedCompProcName, stoppedMeasureProcName, "Aggregated total from (cp1)",
                getClass().getName() + "$2");
        assertThat(output).contains("0.00mW");
        assertThat(output).doesNotContain("Infinity");
    }
}

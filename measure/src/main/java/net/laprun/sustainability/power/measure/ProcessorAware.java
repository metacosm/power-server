package net.laprun.sustainability.power.measure;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.analysis.ComponentProcessor;
import net.laprun.sustainability.power.analysis.DefaultProcessors;
import net.laprun.sustainability.power.analysis.MeasureProcessor;
import net.laprun.sustainability.power.analysis.Processors;

abstract class ProcessorAware {
    private Processors processors = Processors.empty;

    public abstract SensorMetadata metadata();

    public Processors processors() {
        return processors;
    }

    public void registerProcessorFor(int component, ComponentProcessor processor) {
        if (processor != null) {
            if (Processors.empty == processors) {
                processors = new DefaultProcessors(metadata().componentCardinality());
            }
            processors.registerProcessorFor(component, processor);
        }
    }

    public void registerMeasureProcessor(MeasureProcessor processor) {
        if (processor != null) {
            if (Processors.empty == processors) {
                processors = new DefaultProcessors(metadata().componentCardinality());
            }
            processors.registerMeasureProcessor(processor);
        }
    }
}

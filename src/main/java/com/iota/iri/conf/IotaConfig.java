package com.iota.iri.conf;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import java.io.File;

/**
 *  A container for all possible configuration parameters of IRI.
 *  In charge of how we parse the configuration from given inputs.
 */
public abstract class  IotaConfig implements APIConfig, NodeConfig, IXIConfig, DbConfig, ConsensusConfig, ZMQConfig, TipSelConfig, PearlDiverConfig {
    public IotaConfig(){

    }
    public static final File CONFIG_FILE = new File("iota.ini");

    /**
     * Parses the args to populate the configuration object
     *
     * @param args command line args
     * @return {@link JCommander} instance that was used for parsing. It contains metadata about the parsing.
     * @throws ParameterException if the parsing failed
     */
    public abstract JCommander parseConfigFromArgs(String[] args);

    public abstract boolean isHelp();
}

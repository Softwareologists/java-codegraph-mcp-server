package tech.softwareologists.core;

import tech.softwareologists.core.db.EmbeddedNeo4j;
import java.io.File;

/**
 * Default implementation of {@link CoreEngine} using an embedded Neo4j database.
 */
public class CoreEngineImpl implements CoreEngine {
    private final EmbeddedNeo4j db;
    private final QueryService queryService;

    public CoreEngineImpl() {
        this.db = new EmbeddedNeo4j();
        this.queryService = new QueryServiceImpl(db.getDriver());
    }

    @Override
    public void importJar(File jar) {
        JarImporter.importJar(jar, db.getDriver());
    }

    @Override
    public QueryService getQueryService() {
        return queryService;
    }

    @Override
    public String getManifest() {
        return ManifestGenerator.generate();
    }

    @Override
    public void close() {
        db.close();
    }
}

package ca.ucalgary.frs.domain.gateway;

import java.nio.file.Path;

/** Writes generated documents (such as booking confirmations) to disk. */
public interface FileExporter {
    void write(Path target, String content);
}

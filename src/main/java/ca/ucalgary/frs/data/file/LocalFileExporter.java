package ca.ucalgary.frs.data.file;

import ca.ucalgary.frs.domain.exception.PersistenceException;
import ca.ucalgary.frs.domain.gateway.FileExporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes text documents atomically: a temp file in the same folder is moved over the target. */
public final class LocalFileExporter implements FileExporter {
    @Override
    public void write(Path target, String content) {
        Path dir = target.toAbsolutePath().getParent();
        try {
            Path temp = Files.createTempFile(dir, ".frs-", ".tmp");
            try {
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new PersistenceException("Could not save " + target.getFileName() + ": " + e.getMessage(), e);
        }
    }
}

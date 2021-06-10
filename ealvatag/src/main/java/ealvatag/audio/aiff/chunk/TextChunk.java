package ealvatag.audio.aiff.chunk;

import java.io.IOException;
import java.nio.ByteBuffer;

import ealvatag.audio.Utils;
import ealvatag.audio.aiff.AiffAudioHeader;
import ealvatag.audio.iff.Chunk;
import ealvatag.audio.iff.ChunkHeader;
import ealvatag.utils.StandardCharsets;

/**
 * Provides common functionality for textual chunks like {@link NameChunk}, {@link AuthorChunk},
 * {@link CopyrightChunk} and {@link AnnotationChunk}.
 */
public abstract class TextChunk extends Chunk {
    protected final AiffAudioHeader aiffAudioHeader;

    /**
     * Constructor.
     *
     * @param chunkHeader     The header for this chunk
     * @param chunkData       The buffer from which the AIFF data are being read
     * @param aiffAudioHeader aiff header
     */
    public TextChunk(ChunkHeader chunkHeader, ByteBuffer chunkData, AiffAudioHeader aiffAudioHeader) {
        super(chunkData, chunkHeader);
        this.aiffAudioHeader = aiffAudioHeader;
    }

    /**
     * Reads the chunk and transforms it to a {@link String}.
     *
     * @return text string
     * @throws IOException if the read fails
     */
    protected String readChunkText() throws IOException {
        // the spec actually only defines ASCII, not ISO_8859_1, but it probably does not hurt to be lenient
        return Utils.getString(chunkData, 0, chunkData.remaining(), StandardCharsets.ISO_8859_1);
    }

}

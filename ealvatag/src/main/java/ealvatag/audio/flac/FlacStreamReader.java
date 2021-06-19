package ealvatag.audio.flac;

import static ealvatag.logging.EalvaTagLog.LogLevel.WARN;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import ealvatag.audio.Utils;
import ealvatag.audio.exceptions.CannotReadException;
import ealvatag.logging.EalvaTagLog;
import ealvatag.logging.EalvaTagLog.JLogger;
import ealvatag.logging.EalvaTagLog.JLoggers;
import ealvatag.logging.ErrorMessage;
import ealvatag.tag.id3.AbstractID3v2Tag;

/**
 * Flac Stream
 * <p>
 * Reader files and identifies if this is in fact a flac stream
 */
public class FlacStreamReader {
    public static final int FLAC_STREAM_IDENTIFIER_LENGTH = 4;
    public static final String FLAC_STREAM_IDENTIFIER = "fLaC";
    public static JLogger LOG = JLoggers.get(FlacStreamReader.class, EalvaTagLog.MARKER);
    private final FileChannel fc;
    private final String loggingName;
    private int startOfFlacInFile;

    /**
     * Create instance for holding stream info
     *
     * @param fc
     * @param loggingName
     */
    public FlacStreamReader(FileChannel fc, String loggingName) {
        this.fc = fc;
        this.loggingName = loggingName;
    }

    /**
     * Reads the stream block to ensure it is a flac file
     *
     * @throws IOException
     * @throws CannotReadException
     */
    public void findStream() throws IOException, CannotReadException {
        //Begins tag parsing
        if (fc.size() == 0) {
            //Empty File
            throw new CannotReadException("Error: File empty" + " " + loggingName);
        }
        fc.position(0);

        //FLAC Stream at start
        if (isFlacHeader()) {
            startOfFlacInFile = 0;
            return;
        }

        //Ok maybe there is an ID3v24tag first
        if (isId3v2Tag()) {
            startOfFlacInFile = (int) (fc.position() - FLAC_STREAM_IDENTIFIER_LENGTH);
            return;
        }
        throw new CannotReadException(loggingName + ErrorMessage.FLAC_NO_FLAC_HEADER_FOUND);
    }

    private boolean isId3v2Tag() throws IOException {
        fc.position(0);
        if (AbstractID3v2Tag.isId3Tag(fc)) {
            LOG.log(WARN, ErrorMessage.FLAC_CONTAINS_ID3TAG, loggingName, fc.position());
            //FLAC Stream immediately after end of id3 tag
            return isFlacHeader();
        }
        return false;
    }

    private boolean isFlacHeader() throws IOException {
        ByteBuffer headerBuffer = Utils.readFileDataIntoBufferBE(fc, FLAC_STREAM_IDENTIFIER_LENGTH);
        return Utils.readFourBytesAsChars(headerBuffer).equals(FLAC_STREAM_IDENTIFIER);
    }

    /**
     * Usually flac header is at start of file, but unofficially an ID3 tag is allowed at the start of the file.
     *
     * @return the start of the Flac within file
     */
    public int getStartOfFlacInFile() {
        return startOfFlacInFile;
    }
}

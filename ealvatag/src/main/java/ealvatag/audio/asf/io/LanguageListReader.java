package ealvatag.audio.asf.io;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import ealvatag.audio.asf.data.Chunk;
import ealvatag.audio.asf.data.GUID;
import ealvatag.audio.asf.data.LanguageList;
import ealvatag.audio.asf.util.Utils;

/**
 * Reads and interprets the &quot;Language List Object&quot; of ASF files.<br>
 *
 * @author Christian Laireiter
 */
public class LanguageListReader implements ChunkReader {

    /**
     * The GUID this reader {@linkplain #getApplyingIds() applies to}
     */
    private final static GUID[] APPLYING = {GUID.GUID_LANGUAGE_LIST};

    /**
     * {@inheritDoc}
     */
    public boolean canFail() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public GUID[] getApplyingIds() {
        return APPLYING.clone();
    }

    /**
     * {@inheritDoc}
     */
    public Chunk read(GUID guid, InputStream stream, long streamPosition) throws IOException {
        assert GUID.GUID_LANGUAGE_LIST.equals(guid);
        BigInteger chunkLen = Utils.readBig64(stream);

        int readUINT16 = Utils.readUINT16(stream);

        LanguageList result = new LanguageList(streamPosition, chunkLen);
        for (int i = 0; i < readUINT16; i++) {
            int langIdLen = (stream.read() & 0xFF);
            String langId = Utils.readFixedSizeUTF16Str(stream, langIdLen);
            // langIdLen = 2 bytes for each char and optionally one zero
            // termination character
            assert langId.length() == langIdLen / 2 - 1 || langId.length() == langIdLen / 2;
            result.addLanguage(langId);
        }

        return result;
    }

}

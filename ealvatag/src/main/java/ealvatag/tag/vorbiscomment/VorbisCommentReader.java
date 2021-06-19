/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package ealvatag.tag.vorbiscomment;

import static ealvatag.logging.EalvaTagLog.LogLevel.DEBUG;
import static ealvatag.logging.EalvaTagLog.LogLevel.WARN;

import java.io.IOException;

import ealvatag.audio.Utils;
import ealvatag.audio.exceptions.CannotReadException;
import ealvatag.audio.ogg.util.VorbisHeader;
import ealvatag.logging.EalvaTagLog;
import ealvatag.logging.EalvaTagLog.JLogger;
import ealvatag.logging.EalvaTagLog.JLoggers;
import ealvatag.logging.ErrorMessage;

/**
 * Create the VorbisCommentTag by reading from the raw packet data
 * <p>
 * <p>This is in the same format whether encoded with Ogg or Flac
 * except the framing bit is only present when used within Ogg Vorbis
 * <p>
 * <pre>
 * From the http://xiph.org/vorbis/doc/Vorbis_I_spec.html#vorbis-spec-comment
 * Read decodes the packet data using the following algorithm:
 *  [vendor_length] = read an unsigned integer of 32 bits
 *  [vendor_string] = read a UTF-8 vector as [vendor_length] octets
 *  [user_comment_list_length] = read an unsigned integer of 32 bits
 *  iterate [user_comment_list_length] times {
 *      5) [length] = read an unsigned integer of 32 bits
 *      6) this iteration's user comment = read a UTF-8 vector as [length] octets
 *    }
 *  [framing_bit] = read a single bit as boolean
 *  if ( [framing_bit] unset or end-of-packet ) then ERROR
 *  done.
 * </pre>
 */
public class VorbisCommentReader {
    static final int FIELD_COMMENT_LENGTH_LENGTH = 4;
    // Logger Object
    private static final JLogger LOG = JLoggers.get(VorbisCommentReader.class, EalvaTagLog.MARKER);
    //    public static final int FIELD_VENDOR_STRING_POS = 4;
    private static final int FIELD_VENDOR_LENGTH_POS = 0;
    private static final int FIELD_VENDOR_LENGTH_LENGTH = 4;
    private static final int FIELD_USER_COMMENT_LIST_LENGTH = 4;
    /**
     * max comment length that ealvatag can handle, this isnt the maximum column length allowed but we dont
     * dont allow comments larger than this because of problem with allocating memory  (10MB shoudl be fine for all
     * apps)
     */
    private static final int JAUDIOTAGGER_MAX_COMMENT_LENGTH = 10000000;

    public VorbisCommentReader() {

    }

    public VorbisCommentTag read(byte[] rawdata, boolean isFramingBit) throws IOException, CannotReadException {

        VorbisCommentTag tag = new VorbisCommentTag();

        byte[] b = new byte[FIELD_VENDOR_LENGTH_LENGTH];
        System.arraycopy(rawdata, FIELD_VENDOR_LENGTH_POS, b, FIELD_VENDOR_LENGTH_POS, FIELD_VENDOR_LENGTH_LENGTH);
        int pos = FIELD_VENDOR_LENGTH_LENGTH;
        int vendorStringLength = Utils.getIntLE(b);

        b = new byte[vendorStringLength];
        System.arraycopy(rawdata, pos, b, 0, vendorStringLength);
        pos += vendorStringLength;
        tag.setVendor(new String(b, VorbisHeader.CHARSET_UTF_8));
        LOG.log(DEBUG, "Vendor is:%s", tag.getVendor());

        b = new byte[FIELD_USER_COMMENT_LIST_LENGTH];
        System.arraycopy(rawdata, pos, b, 0, FIELD_USER_COMMENT_LIST_LENGTH);
        pos += FIELD_USER_COMMENT_LIST_LENGTH;

        int userComments = Utils.getIntLE(b);
        LOG.log(DEBUG, "Number of user comments:%s", userComments);

        for (int i = 0; i < userComments; i++) {
            b = new byte[FIELD_COMMENT_LENGTH_LENGTH];
            System.arraycopy(rawdata, pos, b, 0, FIELD_COMMENT_LENGTH_LENGTH);
            pos += FIELD_COMMENT_LENGTH_LENGTH;

            int commentLength = Utils.getIntLE(b);
            LOG.log(DEBUG, "Next Comment Length:%s", commentLength);

            if (commentLength > JAUDIOTAGGER_MAX_COMMENT_LENGTH) {
                LOG.log(WARN, ErrorMessage.VORBIS_COMMENT_LENGTH_TOO_LARGE, commentLength);
                break;
            } else if (commentLength > rawdata.length) {
                LOG.log(WARN, ErrorMessage.VORBIS_COMMENT_LENGTH_LARGE_THAN_HEADER, commentLength, rawdata.length);
                break;
            } else {
                b = new byte[commentLength];
                System.arraycopy(rawdata, pos, b, 0, commentLength);
                pos += commentLength;

                VorbisCommentTagField fieldComment = new VorbisCommentTagField(b);
                LOG.log(DEBUG, "Adding:%s", fieldComment.getId());
                tag.addField(fieldComment);
            }
        }

        //Check framing bit, only exists when vorbisComment used within OggVorbis
        if (isFramingBit) {
            if ((rawdata[pos] & 0x01) != 1) {
                throw new CannotReadException(ErrorMessage.OGG_VORBIS_NO_FRAMING_BIT, (rawdata[pos] & 0x01));
            }
        }
        return tag;
    }
}


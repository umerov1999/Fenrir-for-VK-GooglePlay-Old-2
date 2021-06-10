/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
package ealvatag.tag.mp4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import ealvatag.audio.AbstractTagCreator;
import ealvatag.audio.Utils;
import ealvatag.audio.mp4.Mp4AtomIdentifier;
import ealvatag.audio.mp4.atom.Mp4BoxHeader;
import ealvatag.tag.FieldKey;
import ealvatag.tag.TagField;
import ealvatag.tag.TagFieldContainer;
import ealvatag.tag.mp4.field.Mp4TagCoverField;
import ealvatag.utils.StandardCharsets;

/**
 * Create raw content of mp4 tag data, concerns itself with atoms upto the ilst atom
 * <p>
 * <p>This level was selected because the ilst atom can be recreated without reference to existing mp4 fields
 * but fields above this level are dependent upon other information that is not held in the tag.
 * <p>
 * <pre>
 * |--- ftyp
 * |--- moov
 * |......|
 * |......|----- mvdh
 * |......|----- trak
 * |......|----- udta
 * |..............|
 * |..............|-- meta
 * |....................|
 * |....................|-- hdlr
 * |....................|-- ilst
 * |....................|.. ..|
 * |....................|.....|---- @nam (Optional for each metadatafield)
 * |....................|.....|.......|-- data
 * |....................|.....|....... ecetera
 * |....................|.....|---- ---- (Optional for reverse dns field)
 * |....................|.............|-- mean
 * |....................|.............|-- name
 * |....................|.............|-- data
 * |....................|................ ecetere
 * |....................|-- free
 * |--- free
 * |--- mdat
 * </pre>
 */
public class Mp4TagCreator extends AbstractTagCreator {
    /**
     * Convert tagdata to rawdata ready for writing to file
     *
     * @param tag
     * @param padding TODO padding parameter currently ignored
     * @return
     * @throws UnsupportedEncodingException
     */
    public ByteBuffer convert(TagFieldContainer tag, int padding) throws UnsupportedEncodingException {
        try {
            //Add metadata raw content
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<TagField> it = tag.getFields();
            boolean processedArtwork = false;
            while (it.hasNext()) {
                TagField frame = it.next();
                //To ensure order is maintained dont process artwork until iterator hits it.
                if (frame instanceof Mp4TagCoverField) {
                    if (processedArtwork) {
                        //ignore
                    } else {
                        processedArtwork = true;

                        //Because each artwork image is held within the tag as a separate field, but when
                        //they are written they are all held under a single covr box we need to do some checks
                        //and special processing here if we have any artwork image (this code only necessary
                        //if we have more than 1 but do it anyway even if only have 1 image)
                        ByteArrayOutputStream covrDataBaos = new ByteArrayOutputStream();

                        for (TagField artwork : tag.getFields(FieldKey.COVER_ART)) {
                            covrDataBaos.write(((Mp4TagField) artwork).getRawContentDataOnly());
                        }

                        //Now create the parent Data
                        byte[] data = covrDataBaos.toByteArray();
                        baos.write(Utils.getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + data.length));
                        baos.write(Mp4FieldKey.ARTWORK.getFieldName().getBytes(StandardCharsets.ISO_8859_1));
                        baos.write(data);
                    }
                } else {
                    baos.write(frame.getRawContent());
                }
            }

            //Wrap into ilst box
            ByteArrayOutputStream ilst = new ByteArrayOutputStream();
            ilst.write(Utils.getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + baos.size()));
            ilst.write(Mp4AtomIdentifier.ILST.getFieldName().getBytes(StandardCharsets.ISO_8859_1));
            ilst.write(baos.toByteArray());

            //Put into ByteBuffer
            ByteBuffer buf = ByteBuffer.wrap(ilst.toByteArray());
            buf.rewind();
            return buf;
        } catch (IOException ioe) {
            //Should never happen as not writing to file at this point
            throw new RuntimeException(ioe);
        }
    }
}

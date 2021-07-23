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
package ealvatag.audio.ogg.util;

import static ealvatag.logging.EalvaTagLog.LogLevel.DEBUG;
import static ealvatag.logging.EalvaTagLog.LogLevel.WARN;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ealvatag.audio.Utils;
import ealvatag.audio.exceptions.CannotReadException;
import ealvatag.logging.EalvaTagLog;
import ealvatag.logging.EalvaTagLog.JLogger;
import ealvatag.logging.EalvaTagLog.JLoggers;
import ealvatag.logging.ErrorMessage;
import ealvatag.tag.id3.AbstractID3v2Tag;


/**
 * $Id$
 * <p>
 * reference:http://xiph.org/ogg/doc/framing.html
 *
 * @author Raphael Slinckx (KiKiDonK)
 * @version 16 d�cembre 2003
 */
public class OggPageHeader {
    //Capture pattern at start of header
    public static final byte[] CAPTURE_PATTERN = {'O', 'g', 'g', 'S'};
    //Ogg Page header is always 27 bytes plus the size of the segment table which is variable
    public static final int OGG_PAGE_HEADER_FIXED_LENGTH = 27;
    //Can have upto 255 segments in a page
    public static final int MAXIMUM_NO_OF_SEGMENT_SIZE = 255;
    //Each segment can be upto 255 bytes
    public static final int MAXIMUM_SEGMENT_SIZE = 255;
    //Maximum size of page data following the page header (255 * 255 = 65025)
    public static final int MAXIMUM_PAGE_DATA_SIZE = MAXIMUM_NO_OF_SEGMENT_SIZE * MAXIMUM_SEGMENT_SIZE;

    //Maximum size of pageheader (27 + 255 = 282)
//    private static final int MAXIMUM_PAGE_HEADER_SIZE = OGG_PAGE_HEADER_FIXED_LENGTH + MAXIMUM_NO_OF_SEGMENT_SIZE;
    public static final int FIELD_HEADER_TYPE_FLAG_POS = 5;

    //Maximum size of page includes header and data (282 + 65025 = 65307 bytes)
//    public static final int MAXIMUM_PAGE_SIZE = MAXIMUM_PAGE_HEADER_SIZE + MAXIMUM_PAGE_DATA_SIZE;
    public static final int FIELD_PAGE_SEQUENCE_NO_POS = 18;
    public static final int FIELD_PAGE_CHECKSUM_POS = 22;
    static final int FIELD_PAGE_SEGMENTS_POS = 26;
    //Length of various attributes
    static final int FIELD_CAPTURE_PATTERN_LENGTH = 4;
    //Starting positions of the various attributes
//    public static final int FIELD_CAPTURE_PATTERN_POS = 0;
    private static final int FIELD_STREAM_STRUCTURE_VERSION_POS = 4;
    private static final int FIELD_ABSOLUTE_GRANULE_POS = 6;
    private static final int FIELD_STREAM_SERIAL_NO_POS = 14;
    //    public static final int FIELD_SEGMENT_TABLE_POS = 27;
    //    public static final int FIELD_STREAM_STRUCTURE_VERSION_LENGTH = 1;
//    public static final int FIELD_HEADER_TYPE_FLAG_LENGTH = 1;
    private static final int FIELD_ABSOLUTE_GRANULE_LENGTH = 8;
    public static JLogger LOG = JLoggers.get(OggPageHeader.class, EalvaTagLog.MARKER);
    //    public static final int FIELD_STREAM_SERIAL_NO_LENGTH = 4;
//    public static final int FIELD_PAGE_SEQUENCE_NO_LENGTH = 4;
//    public static final int FIELD_PAGE_CHECKSUM_LENGTH = 4;
//    public static final int FIELD_PAGE_SEGMENTS_LENGTH = 1;
    private final byte[] rawHeaderData;
    private final byte headerTypeFlag;
    private final List<PacketStartAndLength> packetList = new ArrayList<>();
    private double absoluteGranulePosition;
    private int checksum;
    private boolean isValid;
    private int pageLength;
    private int pageSequenceNumber, streamSerialNumber;
    private byte[] segmentTable;
    private boolean lastPacketIncomplete;

    private long startByte;

    OggPageHeader(byte[] b) {
        rawHeaderData = b;
        int streamStructureRevision = b[FIELD_STREAM_STRUCTURE_VERSION_POS];
        headerTypeFlag = b[FIELD_HEADER_TYPE_FLAG_POS];
        if (streamStructureRevision == 0) {
            absoluteGranulePosition = 0;
            for (int i = 0; i < FIELD_ABSOLUTE_GRANULE_LENGTH; i++) {
                absoluteGranulePosition += u(b[i + FIELD_ABSOLUTE_GRANULE_POS]) * Math.pow(2, 8 * i);
            }

            streamSerialNumber = Utils.getIntLE(b, FIELD_STREAM_SERIAL_NO_POS, 17);
            pageSequenceNumber = Utils.getIntLE(b, FIELD_PAGE_SEQUENCE_NO_POS, 21);
            checksum = Utils.getIntLE(b, FIELD_PAGE_CHECKSUM_POS, 25);
            u(b[FIELD_PAGE_SEGMENTS_POS]);

            segmentTable = new byte[b.length - OGG_PAGE_HEADER_FIXED_LENGTH];
            int packetLength = 0;
            Integer segmentLength = null;
            for (int i = 0; i < segmentTable.length; i++) {
                segmentTable[i] = b[OGG_PAGE_HEADER_FIXED_LENGTH + i];
                segmentLength = u(segmentTable[i]);
                pageLength += segmentLength;
                packetLength += segmentLength;

                if (segmentLength < MAXIMUM_SEGMENT_SIZE) {
                    packetList.add(new PacketStartAndLength(pageLength - packetLength, packetLength));
                    packetLength = 0;
                }
            }

            //If last segment value is 255 this packet continues onto next page
            //and will not have been added to the packetStartAndEnd list yet
            if (segmentLength != null) {
                if (segmentLength == MAXIMUM_SEGMENT_SIZE) {
                    packetList.add(new PacketStartAndLength(pageLength - packetLength, packetLength));
                    lastPacketIncomplete = true;
                }
            }
            isValid = true;
        }

        LOG.log(DEBUG, "Constructed OggPage:%s", this);
    }

    /**
     * Read next PageHeader from Buffer
     */
    public static OggPageHeader read(ByteBuffer byteBuffer) throws IOException, CannotReadException {
        //byteBuffer
        int start = byteBuffer.position();
        LOG.log(DEBUG, "Trying to read OggPage at:" + start);

        byte[] b = new byte[CAPTURE_PATTERN.length];
        byteBuffer.get(b);
        if (!(Arrays.equals(b, CAPTURE_PATTERN))) {
            throw new CannotReadException(ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND, new String(b));
        }

        byteBuffer.position(start + FIELD_PAGE_SEGMENTS_POS);
        int pageSegments = byteBuffer.get() & 0xFF; //unsigned
        byteBuffer.position(start);

        b = new byte[OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments];
        byteBuffer.get(b);

        //Now just after PageHeader, ready for Packet Data
        return new OggPageHeader(b);
    }

    /**
     * Read next PageHeader from file
     */
    public static OggPageHeader read(RandomAccessFile raf) throws IOException, CannotReadException {
        long start = raf.getFilePointer();
        LOG.log(DEBUG, "Trying to read OggPage at:" + start);

        byte[] b = new byte[CAPTURE_PATTERN.length];
        raf.read(b);
        if (!(Arrays.equals(b, CAPTURE_PATTERN))) {
            raf.seek(start);
            if (AbstractID3v2Tag.isId3Tag(raf)) {
                LOG.log(WARN, ErrorMessage.OGG_CONTAINS_ID3TAG, raf.getFilePointer() - start);
                raf.read(b);
                if ((Arrays.equals(b, CAPTURE_PATTERN))) {
                    //Go to the end of the ID3 header
                    start = raf.getFilePointer() - CAPTURE_PATTERN.length;
                }
            } else {
                throw new CannotReadException(ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND, new String(b));
            }
        }

        raf.seek(start + FIELD_PAGE_SEGMENTS_POS);
        int pageSegments = raf.readByte() & 0xFF; //unsigned
        raf.seek(start);

        b = new byte[OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments];
        raf.read(b);


        OggPageHeader pageHeader = new OggPageHeader(b);
        pageHeader.setStartByte(start);
        //Now just after PageHeader, ready for Packet Data
        return pageHeader;
    }

    private int u(int i) {
        return i & 0xFF;
    }

    /**
     * @return true if the last packet on this page extends to the next page
     */
    public boolean isLastPacketIncomplete() {
        return lastPacketIncomplete;
    }

    double getAbsoluteGranulePosition() {
        LOG.log(DEBUG, "Number Of Samples: " + absoluteGranulePosition);
        return absoluteGranulePosition;
    }


    public int getCheckSum() {
        return checksum;
    }


    public byte getHeaderType() {
        return headerTypeFlag;
    }


    public int getPageLength() {
        LOG.log(DEBUG, "This page length:%s", pageLength);
        return pageLength;
    }

    public int getPageSequence() {
        return pageSequenceNumber;
    }

    public int getSerialNumber() {
        return streamSerialNumber;
    }

    public byte[] getSegmentTable() {
        return segmentTable;
    }

    public boolean isValid() {
        return isValid;
    }

    /**
     * @return a list of packet start position and size within this page.
     */
    public List<PacketStartAndLength> getPacketList() {
        return packetList;
    }

    /**
     * @return the raw header data that this pageheader is derived from
     */
    public byte[] getRawHeaderData() {
        return rawHeaderData;
    }

    public String toString() {
        String out = "Ogg Page Header:isValid:" + isValid + ":type:" + headerTypeFlag + ":oggPageHeaderLength:" +
                rawHeaderData.length + ":length:" + pageLength + ":seqNo:" + getPageSequence() + ":packetIncomplete:" +
                isLastPacketIncomplete() + ":serNum:" + getSerialNumber();

        for (PacketStartAndLength packet : getPacketList()) {
            out += packet.toString();
        }
        return out;
    }

    /**
     * Startbyte of this pageHeader in the file
     * <p>
     * This is useful for Ogg files that contain unsupported additional data at the start of the file such
     * as ID3 data
     */
    public long getStartByte() {
        return startByte;
    }

    private void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    /**
     * This represents all the flags that can be set in the headerType field.
     * Note these values can be ORED together. For example the last packet in
     * a file would normally have a value of 0x5 because both the CONTINUED_PACKET
     * bit and the END_OF_BITSTREAM bit would be set.
     */
    public enum HeaderTypeFlag {
        FRESH_PACKET((byte) 0x0),
        CONTINUED_PACKET((byte) 0x1),
        START_OF_BITSTREAM((byte) 0x2),
        END_OF_BITSTREAM((byte) 0x4);

        byte fileValue;

        HeaderTypeFlag(byte fileValue) {
            this.fileValue = fileValue;
        }

        /**
         * @return the value that should be written to file to enable this flag
         */
        public byte getFileValue() {
            return fileValue;
        }
    }

    /**
     * Within the page specifies the start and length of each packet
     * in the page offset from the end of the pageheader (after the segment table)
     */
    public static class PacketStartAndLength {
        private Integer startPosition = 0;
        private Integer length = 0;

        PacketStartAndLength(int startPosition, int length) {
            this.startPosition = startPosition;
            this.length = length;
        }

//        public int getStartPosition() {
//            return startPosition;
//        }
//
//        public void setStartPosition(int startPosition) {
//            this.startPosition = startPosition;
//        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public String toString() {
            return "NextPkt(start:" + startPosition + ":length:" + length + "),";
        }
    }
}


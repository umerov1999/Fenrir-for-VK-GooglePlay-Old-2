package ealvatag.tag.mp4.field;

import static ealvatag.logging.EalvaTagLog.LogLevel.INFO;
import static ealvatag.logging.EalvaTagLog.LogLevel.WARN;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import ealvatag.audio.Utils;
import ealvatag.audio.mp4.atom.Mp4BoxHeader;
import ealvatag.logging.EalvaTagLog;
import ealvatag.logging.EalvaTagLog.JLogger;
import ealvatag.logging.EalvaTagLog.JLoggers;
import ealvatag.logging.ErrorMessage;
import ealvatag.tag.TagField;
import ealvatag.tag.TagTextField;
import ealvatag.tag.mp4.Mp4FieldKey;
import ealvatag.tag.mp4.Mp4TagField;
import ealvatag.tag.mp4.atom.Mp4DataBox;
import ealvatag.tag.mp4.atom.Mp4MeanBox;
import ealvatag.tag.mp4.atom.Mp4NameBox;
import ealvatag.utils.StandardCharsets;
import okio.Buffer;

/**
 * Represents reverse dns field, used for custom information
 * <p>
 * <p>Originally only used by Itunes for information that was iTunes specific but now used in a wide range of uses,
 * for example Musicbrainz uses it for many of its fields.
 * <p>
 * These fields have a more complex setup
 * Box ----  shows this is a reverse dns metadata field
 * Box mean  the issuer in the form of reverse DNS domain (e.g com.apple.iTunes)
 * Box name  descriptor identifying the type of contents
 * Box data  contents
 * <p>
 * The raw data passed starts from the mean box
 */
public class Mp4TagReverseDnsField extends Mp4TagField implements TagTextField {
    public static final String IDENTIFIER = "----";
    private static final JLogger LOG = JLoggers.get(Mp4TagReverseDnsField.class, EalvaTagLog.MARKER);
    protected int dataSize;
    //Data Content,
    //TODO assuming always text at the moment
    protected String content;
    //Issuer
    private String issuer;
    //Descriptor
    private String descriptor;

    /**
     * Construct from existing file data
     *
     * @param parentHeader
     * @param data
     * @throws UnsupportedEncodingException
     */
    public Mp4TagReverseDnsField(Mp4BoxHeader parentHeader, ByteBuffer data) throws UnsupportedEncodingException {
        super(parentHeader, data);
    }

    /**
     * Newly created Reverse Dns field
     *
     * @param id
     * @param content
     */
    public Mp4TagReverseDnsField(Mp4FieldKey id, String content) {
        super(id.getFieldName());
        issuer = id.getIssuer();
        descriptor = id.getIdentifier();
        this.content = content;
    }

    /**
     * Newly created Reverse Dns field bypassing the Mp4TagField enum for creation of temporary reverse dns fields
     *
     * @param fieldName
     * @param issuer
     * @param identifier
     * @param content
     */
    public Mp4TagReverseDnsField(String fieldName,
                                 String issuer,
                                 String identifier,
                                 String content) {
        super(fieldName);
        this.issuer = issuer;
        descriptor = identifier;
        this.content = content;
    }

    public Mp4TagReverseDnsField(Mp4BoxHeader header, Buffer buffer) throws IOException {
        super(header);
        long enterSize = buffer.size();
        //Read mean box, set the issuer and skip over data
        Mp4BoxHeader meanBoxHeader = new Mp4BoxHeader(buffer);
        Mp4MeanBox meanBox = new Mp4MeanBox(meanBoxHeader, buffer);
        setIssuer(meanBox.getIssuer());

        //Read name box, identify what type of field it is
        Mp4BoxHeader nameBoxHeader = new Mp4BoxHeader(buffer);
        Mp4NameBox nameBox = new Mp4NameBox(nameBoxHeader, buffer);
        setDescriptor(nameBox.getName());

        //Issue 198:There is not actually a data atom there cannot cant be because no room for one
        if (parentHeader.getDataLength() == meanBoxHeader.getLength() + nameBoxHeader.getLength()) {
            id = IDENTIFIER + ":" + issuer + ":" + descriptor;
            setContent("");
            LOG.log(WARN, ErrorMessage.MP4_REVERSE_DNS_FIELD_HAS_NO_DATA, id);
        } else {
            //Usual Case
            //Read data box, identify the data
            Mp4BoxHeader dataBoxHeader = new Mp4BoxHeader(buffer);
            Mp4DataBox dataBox = new Mp4DataBox(dataBoxHeader, buffer);
            setContent(dataBox.getContent());
            //Now calculate the id which in order to be unique needs to use all three values
            id = IDENTIFIER + ":" + issuer + ":" + descriptor;
        }
        long bytesRead = enterSize - buffer.size();
        if (bytesRead < header.getDataLength()) {
            // TODO: 2/4/17 LOG
            buffer.skip(header.getDataLength() - bytesRead);
        }
    }

    @Override
    public Mp4FieldType getFieldType() {
        //TODO always assuming text at moment but may not always be the case (though dont have any concrete
        //examples)
        return Mp4FieldType.TEXT;
    }

    @Override
    protected void build(ByteBuffer data) throws UnsupportedEncodingException {
        //Read mean box, set the issuer and skip over data
        Mp4BoxHeader meanBoxHeader = new Mp4BoxHeader(data);
        Mp4MeanBox meanBox = new Mp4MeanBox(meanBoxHeader, data);
        setIssuer(meanBox.getIssuer());
        data.position(data.position() + meanBoxHeader.getDataLength());

        //Read name box, identify what type of field it is
        Mp4BoxHeader nameBoxHeader = new Mp4BoxHeader(data);
        Mp4NameBox nameBox = new Mp4NameBox(nameBoxHeader, data);
        setDescriptor(nameBox.getName());
        data.position(data.position() + nameBoxHeader.getDataLength());

        //Issue 198:There is not actually a data atom there cannot cant be because no room for one
        if (parentHeader.getDataLength() == meanBoxHeader.getLength() + nameBoxHeader.getLength()) {
            id = IDENTIFIER + ":" + issuer + ":" + descriptor;
            setContent("");
            LOG.log(WARN, ErrorMessage.MP4_REVERSE_DNS_FIELD_HAS_NO_DATA, id);
        }
        //Usual Case
        else {
            //Read data box, identify the data
            Mp4BoxHeader dataBoxHeader = new Mp4BoxHeader(data);
            Mp4DataBox dataBox = new Mp4DataBox(dataBoxHeader, data);
            setContent(dataBox.getContent());
            data.position(data.position() + dataBoxHeader.getDataLength());

            //Now calculate the id which in order to be unique needs to use all htree values
            id = IDENTIFIER + ":" + issuer + ":" + descriptor;
        }
    }

    @Override
    public void copyContent(TagField field) {
        if (field instanceof Mp4TagReverseDnsField) {
            issuer = ((Mp4TagReverseDnsField) field).getIssuer();
            descriptor = ((Mp4TagReverseDnsField) field).getDescriptor();
            content = ((Mp4TagReverseDnsField) field).getContent();
        }
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(String s) {
        content = s;
    }

    @Override
    protected byte[] getDataBytes() throws UnsupportedEncodingException {
        return content.getBytes(getEncoding());
    }

    @Override
    public Charset getEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public void setEncoding(Charset s) {
        /* Not allowed */
    }

    /**
     * Convert back to raw content, includes ----,mean,name and data atom as views as one thing externally
     *
     * @return
     * @throws UnsupportedEncodingException
     */
    @Override
    public byte[] getRawContent() throws UnsupportedEncodingException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //Create Meanbox data
            byte[] issuerRawData = issuer.getBytes(getEncoding());
            baos.write(Utils.getSizeBEInt32(
                    Mp4BoxHeader.HEADER_LENGTH + Mp4MeanBox.PRE_DATA_LENGTH + issuerRawData.length));
            baos.write(Mp4MeanBox.IDENTIFIER.getBytes(StandardCharsets.ISO_8859_1));
            baos.write(new byte[]{0, 0, 0, 0});
            baos.write(issuerRawData);

            //Create Namebox data
            byte[] nameRawData = descriptor.getBytes(getEncoding());
            baos.write(Utils.getSizeBEInt32(
                    Mp4BoxHeader.HEADER_LENGTH + Mp4NameBox.PRE_DATA_LENGTH + nameRawData.length));
            baos.write(Mp4NameBox.IDENTIFIER.getBytes(StandardCharsets.ISO_8859_1));
            baos.write(new byte[]{0, 0, 0, 0});
            baos.write(nameRawData);

            //Create DataBox data if we have data only
            if (content.length() > 0) {
                baos.write(getRawContentDataOnly());
            }
            //Now wrap with reversedns box
            ByteArrayOutputStream outerbaos = new ByteArrayOutputStream();
            outerbaos.write(Utils.getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + baos.size()));
            outerbaos.write(IDENTIFIER.getBytes(StandardCharsets.ISO_8859_1));
            outerbaos.write(baos.toByteArray());
            return outerbaos.toByteArray();

        } catch (IOException ioe) {
            //This should never happen as were not actually writing to/from a file
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public byte[] getRawContentDataOnly() throws UnsupportedEncodingException {
        LOG.log(INFO, "Getting Raw data for:" + getId());
        try {
            //Create DataBox data
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] dataRawData = content.getBytes(getEncoding());
            baos.write(Utils.getSizeBEInt32(
                    Mp4BoxHeader.HEADER_LENGTH + Mp4DataBox.PRE_DATA_LENGTH + dataRawData.length));
            baos.write(Mp4DataBox.IDENTIFIER.getBytes(StandardCharsets.ISO_8859_1));
            baos.write(new byte[]{0});
            baos.write(new byte[]{0, 0, (byte) getFieldType().getFileClassId()});
            baos.write(new byte[]{0, 0, 0, 0});
            baos.write(dataRawData);
            return baos.toByteArray();
        } catch (IOException ioe) {
            //This should never happen as were not actually writing to/from a file
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return "".equals(content.trim());
    }

    @Override
    public String toString() {
        return content;
    }

    /**
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Set the issuer, usually reverse dns of the Companies domain
     *
     * @param issuer
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * @return the descriptor
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * Set the descriptor for the data (what type of data it is)
     *
     * @param descriptor
     */
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
}

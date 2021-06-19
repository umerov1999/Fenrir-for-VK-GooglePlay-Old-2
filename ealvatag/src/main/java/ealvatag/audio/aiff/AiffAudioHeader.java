package ealvatag.audio.aiff;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ealvatag.audio.GenericAudioHeader;

/**
 * Non-"tag" metadata from the AIFF file. In general, read-only.
 */
public class AiffAudioHeader extends GenericAudioHeader {

    private final List<String> applicationIdentifiers;
    private final List<String> comments;
    private final List<String> annotations;
    private AiffType fileType;
    private Date timestamp;
    private Endian endian;
    private String audioEncoding;
    private String name;
    private String author;
    private String copyright;

    public AiffAudioHeader() {
        applicationIdentifiers = new ArrayList<String>();
        comments = new ArrayList<String>();
        annotations = new ArrayList<String>();
        endian = Endian.BIG_ENDIAN;
    }

    /**
     * Return the timestamp of the file.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp.
     */
    public void setTimestamp(Date d) {
        timestamp = d;
    }

    /**
     * Return the file type (AIFF or AIFC)
     */
    public AiffType getFileType() {
        return fileType;
    }

    /**
     * Set the file type (AIFF or AIFC)
     */
    public void setFileType(AiffType typ) {
        fileType = typ;
    }

    /**
     * Return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Set the author
     */
    public void setAuthor(String a) {
        author = a;
    }

    /**
     * Return the name. May be null.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name
     */
    public void setName(String n) {
        name = n;
    }

    /**
     * Return the copyright. May be null.
     */
    public String getCopyright() {
        return copyright;
    }

    /**
     * Set the copyright
     */
    public void setCopyright(String c) {
        copyright = c;
    }

    /**
     * Return endian status (big or little)
     */
    public Endian getEndian() {
        return endian;
    }

    /**
     * Set endian status (big or little)
     */
    public void setEndian(Endian e) {
        endian = e;
    }

    /**
     * Return list of all application identifiers
     */
    public List<String> getApplicationIdentifiers() {
        return applicationIdentifiers;
    }

    /**
     * Add an application identifier. There can be any number of these.
     */
    public void addApplicationIdentifier(String id) {
        applicationIdentifiers.add(id);
    }

    /**
     * Return list of all annotations
     */
    public List<String> getAnnotations() {
        return annotations;
    }

    /**
     * Add an annotation. There can be any number of these.
     */
    public void addAnnotation(String a) {
        annotations.add(a);
    }

    /**
     * Return list of all comments
     */
    public List<String> getComments() {
        return comments;
    }

    /**
     * Add a comment. There can be any number of these.
     */
    public void addComment(String c) {
        comments.add(c);
    }

    @Override
    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("fileType", fileType)
                .add("timestamp", timestamp)
                .add("endian", endian)
                .add("audioEncoding", audioEncoding)
                .add("name", name)
                .add("author", author)
                .add("copyright", copyright)
                .add("applicationIdentifiers", applicationIdentifiers)
                .add("comments", comments)
                .add("annotations", annotations);
    }


    public enum Endian {
        BIG_ENDIAN,
        LITTLE_ENDIAN
    }
}

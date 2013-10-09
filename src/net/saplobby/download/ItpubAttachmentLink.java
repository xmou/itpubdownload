package net.saplobby.download;

public class ItpubAttachmentLink implements Comparable<ItpubAttachmentLink> {
    private String link;
    private String name;

    public ItpubAttachmentLink(String link, String name) {
        this.link = link;
        setName(name);
    }

    public String getLink() {
        return this.link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        name = name.replace("&#039;", "'");
        name = name.replace("\\", "");
        this.name = name;
    }

    public String toString() {
        return "net.saplobby.download.ItpubAttachmentLink [link=" + link + ", name=" + name + "]";
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (link == null ? 0 : link.hashCode());
        result = 31 * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ItpubAttachmentLink other = (ItpubAttachmentLink) obj;
        if (this.link == null) {
            if (other.link != null)
                return false;
        } else if (!this.link.equals(other.link))
            return false;
        if (this.name == null) {
            if (other.name != null)
                return false;
        } else if (!this.name.equals(other.name))
            return false;
        return true;
    }

    public int compareTo(ItpubAttachmentLink o) {
        return this.name.compareTo(o.name);
    }
}

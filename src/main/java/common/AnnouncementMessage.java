package common;

public class AnnouncementMessage extends BrokerMessage {
    private String announcement;

    public AnnouncementMessage(String announcement) {
        super(Type.ANNOUNCEMENT);
        this.announcement = announcement;
    }

    public String getAnnouncemnet() {
        return announcement;
    }
}

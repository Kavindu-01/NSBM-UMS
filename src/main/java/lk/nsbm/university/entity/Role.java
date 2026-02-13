package lk.nsbm.university.entity;

public enum Role {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Administrator"),
    MODERATOR("Moderator"),
    CONTENT_MANAGER("Content Manager"),
    EDITOR("Editor"),
    BOARDING_GUARDIAN("Boarding Guardian"),
    SHUTTLE_DRIVER("Shuttle Driver"),
    CLUB_PRESIDENT("Club President"),
    USER("Student");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

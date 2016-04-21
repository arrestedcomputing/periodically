package com.aubray.periodically.model;

public class Invitation {
    private String inviterUid;
    private String inviteeUid;
    private String periodicalId;

    public Invitation() {}

    public Invitation(String inviterUid, String inviteeUid, String periodicalId) {
        this.inviterUid = inviterUid;
        this.inviteeUid = inviteeUid;
        this.periodicalId = periodicalId;
    }

    public String getInviterUid() {
        return inviterUid;
    }

    public String getInviteeUid() {
        return inviteeUid;
    }

    public String getPeriodicalId() {
        return periodicalId;
    }
}

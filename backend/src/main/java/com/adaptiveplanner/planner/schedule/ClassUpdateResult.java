package com.adaptiveplanner.planner.schedule;

import java.time.LocalDate;

public record ClassUpdateResult(ClassOutcome outcome, String proposalId, LocalDate proposedDate,
                                String message) {
    public boolean hasProposal() { return proposalId != null; }
}

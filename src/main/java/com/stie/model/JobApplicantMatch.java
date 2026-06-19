package com.stie.model;

/**
 * Transient DTO — holds a Candidate alongside computed match data for a specific job.
 * Not persisted — built at runtime by JobController.
 */
public class JobApplicantMatch {

    private final Candidate candidate;

    /** 0–100 overall match score */
    private final int matchScore;

    /** How many of the required skills the candidate has */
    private final int matchedSkillCount;

    /** Total required skills on the job */
    private final int totalRequiredSkills;

    /** Candidate meets or exceeds the experience requirement */
    private final boolean experienceMet;

    /** Points from skill matches (partial) */
    private final int skillPoints;

    /** Points from experience match (partial) */
    private final int expPoints;

    public JobApplicantMatch(Candidate candidate, int matchScore, int matchedSkillCount,
                             int totalRequiredSkills, boolean experienceMet,
                             int skillPoints, int expPoints) {
        this.candidate = candidate;
        this.matchScore = matchScore;
        this.matchedSkillCount = matchedSkillCount;
        this.totalRequiredSkills = totalRequiredSkills;
        this.experienceMet = experienceMet;
        this.skillPoints = skillPoints;
        this.expPoints = expPoints;
    }

    public Candidate getCandidate()          { return candidate; }
    public int getMatchScore()               { return matchScore; }
    public int getMatchedSkillCount()        { return matchedSkillCount; }
    public int getTotalRequiredSkills()      { return totalRequiredSkills; }
    public boolean isExperienceMet()         { return experienceMet; }
    public int getSkillPoints()              { return skillPoints; }
    public int getExpPoints()                { return expPoints; }
}

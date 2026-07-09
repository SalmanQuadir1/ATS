package com.stie.service;

import com.stie.model.Interview;
import com.stie.model.InterviewScorecard;
import com.stie.repository.InterviewRepository;
import com.stie.repository.InterviewScorecardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScorecardService {

    @Autowired
    private InterviewScorecardRepository scorecardRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    public void saveScorecard(InterviewScorecard scorecard) {
        scorecardRepository.save(scorecard);

        // Compute average across all scorecards for this interview and update target Interview averages
        Interview interview = scorecard.getInterview();
        List<InterviewScorecard> scorecards = scorecardRepository.findByInterviewId(interview.getId());

        if (!scorecards.isEmpty()) {
            double sumAvg = 0;

            for (InterviewScorecard sc : scorecards) {
                sumAvg += sc.getAverageScore();
            }

            int count = scorecards.size();
            int overallAvg = (int) Math.round(sumAvg / count);
            
            interview.setTechnicalScore(overallAvg);
            interview.setCommunicationScore(overallAvg);
            interview.setCultureScore(overallAvg);
            
            // Generate combined feedback
            StringBuilder combinedFeed = new StringBuilder();
            combinedFeed.append("Panel Averages Compiled from ").append(count).append(" Interviewer(s):\n");
            for (InterviewScorecard sc : scorecards) {
                combinedFeed.append("- [").append(sc.getSubmitter()).append("]: ").append(sc.getComments()).append("\n");
            }
            interview.setFeedback(combinedFeed.toString());
            interviewRepository.save(interview);
        }
    }

    public List<InterviewScorecard> getScorecardsByInterview(Long interviewId) {
        return scorecardRepository.findByInterviewId(interviewId);
    }

    public List<InterviewScorecard> getScorecardsByCandidate(Long candidateId) {
        return scorecardRepository.findByInterviewCandidateId(candidateId);
    }
}


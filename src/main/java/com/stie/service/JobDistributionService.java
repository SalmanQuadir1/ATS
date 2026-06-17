package com.stie.service;

import com.stie.model.JobVacancy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Distributes job vacancies to external job boards.
 *
 * API credentials are configured in application.properties.
 * If a credential is blank the method returns false gracefully
 * so the UI can display a "not configured" warning.
 */
@Service
public class JobDistributionService {

    private static final Logger log = Logger.getLogger(JobDistributionService.class.getName());

    private final RestTemplate restTemplate = new RestTemplate();

    // ── LinkedIn ─────────────────────────────────────────────────────────────
    @Value("${linkedin.access.token:}")
    private String linkedInAccessToken;

    @Value("${linkedin.organization.id:}")
    private String linkedInOrgId;

    // ── JobStreet / SEEK ──────────────────────────────────────────────────────
    @Value("${jobstreet.api.key:}")
    private String jobStreetApiKey;

    @Value("${jobstreet.advertiser.id:}")
    private String jobStreetAdvertiserId;

    // ── Indeed ────────────────────────────────────────────────────────────────
    @Value("${indeed.publisher.key:}")
    private String indeedPublisherKey;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Posts a job vacancy to LinkedIn via the Job Postings API.
     * Requires a valid access token and organization ID.
     */
    public boolean postToLinkedIn(JobVacancy job) {
        if (linkedInAccessToken == null || linkedInAccessToken.trim().isEmpty()) {
            log.warning("[JobDistribution] LinkedIn access token not configured. Skipping.");
            return false;
        }
        if (linkedInOrgId == null || linkedInOrgId.trim().isEmpty()) {
            log.warning("[JobDistribution] LinkedIn organization ID not configured. Skipping.");
            return false;
        }

        try {
            String url = "https://api.linkedin.com/v2/simpleJobPostings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(linkedInAccessToken.trim());
            headers.set("X-Restli-Protocol-Version", "2.0.0");

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("title", job.getTitle());
            body.put("description", job.getDescription() != null ? job.getDescription() : "");
            body.put("companyApplyUrl", "");
            body.put("listedAt", System.currentTimeMillis());

            Map<String, String> company = new HashMap<String, String>();
            company.put("companyUrn", "urn:li:organization:" + linkedInOrgId.trim());
            body.put("company", company);

            HttpEntity<Map<String, Object>> request = new HttpEntity<Map<String, Object>>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("[JobDistribution] LinkedIn response: " + response.getStatusCode());
            return success;

        } catch (Exception e) {
            log.warning("[JobDistribution] LinkedIn post failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Posts a job vacancy to JobStreet (SEEK API).
     * Requires a valid API key and advertiser ID.
     */
    public boolean postToJobStreet(JobVacancy job) {
        if (jobStreetApiKey == null || jobStreetApiKey.trim().isEmpty()) {
            log.warning("[JobDistribution] JobStreet API key not configured. Skipping.");
            return false;
        }
        if (jobStreetAdvertiserId == null || jobStreetAdvertiserId.trim().isEmpty()) {
            log.warning("[JobDistribution] JobStreet advertiser ID not configured. Skipping.");
            return false;
        }

        try {
            String url = "https://api.seek.com.au/v2/advertisers/"
                    + jobStreetAdvertiserId.trim() + "/jobs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + jobStreetApiKey.trim());
            headers.set("User-Agent", "STIE-Enterprise/1.0");

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("title", job.getTitle());
            body.put("summary", job.getDescription() != null
                    ? job.getDescription().substring(0, Math.min(job.getDescription().length(), 500))
                    : "");
            body.put("advertiserId", jobStreetAdvertiserId.trim());

            Map<String, String> location = new HashMap<String, String>();
            location.put("countryCode", "SG");
            body.put("location", location);

            HttpEntity<Map<String, Object>> request = new HttpEntity<Map<String, Object>>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("[JobDistribution] JobStreet response: " + response.getStatusCode());
            return success;

        } catch (Exception e) {
            log.warning("[JobDistribution] JobStreet post failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Posts a job vacancy to Indeed via the Publisher API.
     * Requires a valid publisher key.
     */
    public boolean postToIndeed(JobVacancy job) {
        if (indeedPublisherKey == null || indeedPublisherKey.trim().isEmpty()) {
            log.warning("[JobDistribution] Indeed publisher key not configured. Skipping.");
            return false;
        }

        try {
            String url = "https://apis.indeed.com/v2/jobs";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(indeedPublisherKey.trim());

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("title", job.getTitle());
            body.put("description", job.getDescription() != null ? job.getDescription() : "");
            body.put("reqId", String.valueOf(job.getId()));
            body.put("country", "SG");
            body.put("location", "Singapore");
            if (job.getDepartment() != null) {
                body.put("category", job.getDepartment());
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<Map<String, Object>>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("[JobDistribution] Indeed response: " + response.getStatusCode());
            return success;

        } catch (Exception e) {
            log.warning("[JobDistribution] Indeed post failed: " + e.getMessage());
            return false;
        }
    }
}



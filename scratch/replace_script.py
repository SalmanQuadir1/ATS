import os
import re


def process_html_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        html = f.read()

    # We only want to replace in:
    # 1. Text nodes
    # 2. 'placeholder' attributes
    # 3. 'title' attributes

    # Using regex to replace outside of tags is hard, but we can do a simple heuristic:
    # Split by '<' and '>'
    
    # Actually, BeautifulSoup is safer if available, but it might mess up Thymeleaf namespaces.
    # Let's use regex that finds text between > and <
    
    def replace_text(match):
        text = match.group(0)
        # Don't replace if it's inside a Thymeleaf expression ${...} or @{...} or *{...}
        # A simple way: just replace Candidate(s) but skip if the word is part of a camelCase variable like candidateId
        
        # Replace capitalized
        text = re.sub(r'\bCandidates\b', 'Applicants', text)
        text = re.sub(r'\bCandidate\b', 'Applicant', text)
        
        # Replace lowercase (only if it's a standalone word, not candidateId or my_candidate)
        text = re.sub(r'\bcandidates\b(?![-_A-Z])', 'applicants', text)
        text = re.sub(r'\bcandidate\b(?![-_A-Z])', 'applicant', text)
        
        return text

    # Regex to find everything between > and <
    # >...<
    new_html = re.sub(r'>([^<]+)<', replace_text, html)

    # Also we want to replace in placeholder="Search candidates..."
    def replace_placeholder(match):
        attr = match.group(0)
        attr = re.sub(r'\bCandidates\b', 'Applicants', attr)
        attr = re.sub(r'\bCandidate\b', 'Applicant', attr)
        attr = re.sub(r'\bcandidates\b', 'applicants', attr)
        attr = re.sub(r'\bcandidate\b', 'applicant', attr)
        return attr

    new_html = re.sub(r'placeholder="[^"]+"', replace_placeholder, new_html)
    new_html = re.sub(r'title="[^"]+"', replace_placeholder, new_html)

    # Note: we should avoid replacing in th:text="${candidate.name}" which is inside an attribute, but we only target >...< and placeholder/title, so we are safe!
    
    # We should also replace some hardcoded text in th:text="'...'" if any, but that's harder. Let's just do >...< first.

    if new_html != html:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_html)
        print(f"Updated {filepath}")

dir_path = r'c:\Users\Asus\OneDrive\Desktop\ATS\src\main\resources\templates'

for root, dirs, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.html'):
            process_html_file(os.path.join(root, file))

print("Done")

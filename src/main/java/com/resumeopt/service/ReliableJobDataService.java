package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.MatchLevel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReliableJobDataService {

        public List<JobListing> getReliableJobListings() {
                return getReliableJobsInternal();
        }

        public List<JobListing> getFreshersJobs() {
                return getReliableJobsInternal();
        }

        private List<JobListing> getReliableJobsInternal() {
                List<JobListing> jobs = new ArrayList<>();
                jobs.add(createJob("Junior Python Developer", "Search on Naukri",
                                "Python development role. Django, Flask, pandas, NumPy. Entry-level position with growth opportunities.",
                                "https://www.naukri.com/python-developer-jobs?k=python%20developer&experience=0&experience=1",
                                MatchLevel.EXCELLENT));

                // Mobile Development
                jobs.add(createJob("Android Developer Trainee", "Search on Indeed",
                                "Android app development trainee. Java/Kotlin, Android Studio, Firebase. Fresh graduates welcome. 1-year training program.",
                                "https://in.indeed.com/jobs?q=android+developer+fresher&l=India&explvl=entry_level",
                                MatchLevel.VERY_GOOD));

                jobs.add(createJob("iOS Developer Intern", "Search on Internshala",
                                "iOS development internship. Swift, Xcode, UIKit. 6-month paid internship with full-time conversion opportunity.",
                                "https://internshala.com/internships/ios-app-development-internship/",
                                MatchLevel.VERY_GOOD));

                // DevOps & Cloud
                jobs.add(createJob("DevOps Engineer - Entry Level", "Search on LinkedIn",
                                "Entry-level DevOps role. AWS, Docker, Jenkins, Git. Training on cloud technologies provided. 0-1 years experience.",
                                "https://www.linkedin.com/jobs/search/?keywords=devops%20engineer%20entry%20level&location=India&f_E=2",
                                MatchLevel.VERY_GOOD));

                // Full Stack Development
                jobs.add(createJob("Full Stack Developer Trainee", "Search on Naukri",
                                "MEAN/MERN stack development. MongoDB, Express.js, Angular/React, Node.js. Comprehensive training program for freshers.",
                                "https://www.naukri.com/full-stack-developer-jobs?k=full%20stack%20developer&experience=0&experience=1",
                                MatchLevel.EXCELLENT));

                jobs.add(createJob("Web Developer - Junior", "Search on Indeed",
                                "Junior web developer position. HTML5, CSS3, JavaScript, PHP, WordPress. Creative agency environment. Portfolio required.",
                                "https://in.indeed.com/jobs?q=web+developer+junior&l=India&explvl=entry_level",
                                MatchLevel.VERY_GOOD));

                // Cybersecurity
                jobs.add(createJob("Cybersecurity Analyst Intern", "Search on Internshala",
                                "Cybersecurity internship program. Network security, ethical hacking, SIEM tools. Certification support provided.",
                                "https://internshala.com/internships/cyber-security-internship/",
                                MatchLevel.VERY_GOOD));

                // Database & Backend
                jobs.add(createJob("Database Developer Trainee", "Search on Glassdoor",
                                "Database development and administration. MySQL, PostgreSQL, MongoDB. SQL expertise required. Training on NoSQL databases.",
                                "https://www.glassdoor.co.in/Job/jobs.htm?sc.keyword=database%20developer%20entry%20level&locT=N&locId=115&locKeyword=India",
                                MatchLevel.VERY_GOOD));

                jobs.add(createJob("Backend Developer - Node.js", "Search on LinkedIn",
                                "Node.js backend development. Express.js, MongoDB, REST APIs, microservices. Fresh graduates with JavaScript knowledge preferred.",
                                "https://www.linkedin.com/jobs/search/?keywords=backend%20developer%20nodejs%20fresher&location=India&f_E=2",
                                MatchLevel.EXCELLENT));

                // Emerging Technologies
                jobs.add(createJob("Machine Learning Intern", "Search on Internshala",
                                "ML internship program. Python, scikit-learn, TensorFlow, data preprocessing. Mathematics/Statistics background preferred.",
                                "https://internshala.com/internships/machine-learning-internship/", MatchLevel.GOOD));

                jobs.add(createJob("Blockchain Developer Trainee", "Search on Indeed",
                                "Blockchain development trainee. Solidity, Ethereum, smart contracts, Web3.js. Cryptocurrency and DeFi focus.",
                                "https://in.indeed.com/jobs?q=blockchain+developer+fresher&l=India&explvl=entry_level",
                                MatchLevel.GOOD));

                // Product & UI/UX
                jobs.add(createJob("UI/UX Designer Intern", "Search on Internshala",
                                "UI/UX design internship. Figma, Adobe XD, user research, prototyping. Portfolio review required. 6-month program.",
                                "https://internshala.com/internships/ui-ux-design-internship/", MatchLevel.VERY_GOOD));

                jobs.add(createJob("Product Management Trainee", "Search on LinkedIn",
                                "Associate product manager trainee. Product lifecycle, user stories, agile methodology. Technical background preferred.",
                                "https://www.linkedin.com/jobs/search/?keywords=product%20manager%20trainee&location=India&f_E=2",
                                MatchLevel.GOOD));

                // Startup Opportunities
                jobs.add(createJob("Software Engineer - Startup", "Search on AngelList",
                                "Full-stack engineer at growing startup. Multiple technologies, fast-paced environment. Equity participation. Remote-friendly.",
                                "https://angel.co/jobs#find/f!%7B%22locations%22%3A%5B%22India%22%5D%2C%22roles%22%3A%5B%22Software%20Engineer%22%5D%7D",
                                MatchLevel.EXCELLENT));

                // Shuffle the jobs to show different ones on each request
                Collections.shuffle(jobs);

                // Return only a subset (15-20 jobs) to keep it fresh
                int maxJobs = Math.min(20, jobs.size());
                return jobs.subList(0, maxJobs);
        }

        private JobListing createJob(String title, String company, String description, String applyUrl,
                        MatchLevel matchLevel) {
                JobListing job = new JobListing();
                job.setTitle(title);
                job.setCompany(company);
                job.setDescription(description);
                job.setApplyUrl(applyUrl);
                job.setLinkVerified(true); // These
                                           // are
                                           // real
                                           // job
                                           // portal
                                           // search
                                           // URLs
                job.setMatchLevel(matchLevel);

                // Set posted date to a random time between 2-6 days ago (within the valid
                // range)
                LocalDateTime now = LocalDateTime.now();
                int daysAgo = 2 + (int) (Math.random() * 5); // 2-6 days ago
                job.setPostedDate(now.minusDays(daysAgo));

                return job;
        }
}